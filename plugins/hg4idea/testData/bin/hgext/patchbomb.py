# patchbomb.py - sending Mercurial changesets as patch emails
#
#  Copyright 2005-2009 Matt Mackall <mpm@selenic.com> and others
#
# This software may be used and distributed according to the terms of the
# GNU General Public License version 2 or any later version.

'''command to send changesets as (a series of) patch emails

The series is started off with a "[PATCH 0 of N]" introduction, which
describes the series as a whole.

Each patch email has a Subject line of "[PATCH M of N] ...", using the
first line of the changeset description as the subject text. The
message contains two or three body parts:

- The changeset description.
- [Optional] The result of running diffstat on the patch.
- The patch itself, as generated by "hg export".

Each message refers to the first in the series using the In-Reply-To
and References headers, so they will show up as a sequence in threaded
mail and news readers, and in mail archives.

With the -d/--diffstat option, you will be prompted for each changeset
with a diffstat summary and the changeset summary, so you can be sure
you are sending the right changes.

To configure other defaults, add a section like this to your hgrc
file::

  [email]
  from = My Name <my@email>
  to = recipient1, recipient2, ...
  cc = cc1, cc2, ...
  bcc = bcc1, bcc2, ...

Use ``[patchbomb]`` as configuration section name if you need to
override global ``[email]`` address settings.

Then you can use the "hg email" command to mail a series of changesets
as a patchbomb.

To avoid sending patches prematurely, it is a good idea to first run
the "email" command with the "-n" option (test only). You will be
prompted for an email recipient address, a subject and an introductory
message describing the patches of your patchbomb. Then when all is
done, patchbomb messages are displayed. If the PAGER environment
variable is set, your pager will be fired up once for each patchbomb
message, so you can verify everything is alright.

The -m/--mbox option is also very useful. Instead of previewing each
patchbomb message in a pager or sending the messages directly, it will
create a UNIX mailbox file with the patch emails. This mailbox file
can be previewed with any mail user agent which supports UNIX mbox
files, e.g. with mutt::

  % mutt -R -f mbox

When you are previewing the patchbomb messages, you can use ``formail``
(a utility that is commonly installed as part of the procmail
package), to send each message out::

  % formail -s sendmail -bm -t < mbox

That should be all. Now your patchbomb is on its way out.

You can also either configure the method option in the email section
to be a sendmail compatible mailer or fill out the [smtp] section so
that the patchbomb extension can automatically send patchbombs
directly from the commandline. See the [email] and [smtp] sections in
hgrc(5) for details.
'''

import os, errno, socket, tempfile, cStringIO, time
import email.MIMEMultipart, email.MIMEBase
import email.Utils, email.Encoders, email.Generator
from mercurial import cmdutil, commands, hg, mail, patch, util
from mercurial.i18n import _
from mercurial.node import bin

def prompt(ui, prompt, default=None, rest=':'):
    if not ui.interactive():
        if default is not None:
            return default
        raise util.Abort(_("%s Please enter a valid value" % (prompt + rest)))
    if default:
        prompt += ' [%s]' % default
    prompt += rest
    while True:
        r = ui.prompt(prompt, default=default)
        if r:
            return r
        if default is not None:
            return default
        ui.warn(_('Please enter a valid value.\n'))

def cdiffstat(ui, summary, patchlines):
    s = patch.diffstat(patchlines)
    if summary:
        ui.write(summary, '\n')
        ui.write(s, '\n')
    ans = prompt(ui, _('does the diffstat above look okay?'), 'y')
    if not ans.lower().startswith('y'):
        raise util.Abort(_('diffstat rejected'))
    return s

def makepatch(ui, repo, patch, opts, _charsets, idx, total, patchname=None):

    desc = []
    node = None
    body = ''

    for line in patch:
        if line.startswith('#'):
            if line.startswith('# Node ID'):
                node = line.split()[-1]
            continue
        if line.startswith('diff -r') or line.startswith('diff --git'):
            break
        desc.append(line)

    if not patchname and not node:
        raise ValueError

    if opts.get('attach'):
        body = ('\n'.join(desc[1:]).strip() or
                'Patch subject is complete summary.')
        body += '\n\n\n'

    if opts.get('plain'):
        while patch and patch[0].startswith('# '):
            patch.pop(0)
        if patch:
            patch.pop(0)
        while patch and not patch[0].strip():
            patch.pop(0)

    if opts.get('diffstat'):
        body += cdiffstat(ui, '\n'.join(desc), patch) + '\n\n'

    if opts.get('attach') or opts.get('inline'):
        msg = email.MIMEMultipart.MIMEMultipart()
        if body:
            msg.attach(mail.mimeencode(ui, body, _charsets, opts.get('test')))
        p = mail.mimetextpatch('\n'.join(patch), 'x-patch', opts.get('test'))
        binnode = bin(node)
        # if node is mq patch, it will have the patch file's name as a tag
        if not patchname:
            patchtags = [t for t in repo.nodetags(binnode)
                         if t.endswith('.patch') or t.endswith('.diff')]
            if patchtags:
                patchname = patchtags[0]
            elif total > 1:
                patchname = cmdutil.make_filename(repo, '%b-%n.patch',
                                                  binnode, seqno=idx, total=total)
            else:
                patchname = cmdutil.make_filename(repo, '%b.patch', binnode)
        disposition = 'inline'
        if opts.get('attach'):
            disposition = 'attachment'
        p['Content-Disposition'] = disposition + '; filename=' + patchname
        msg.attach(p)
    else:
        body += '\n'.join(patch)
        msg = mail.mimetextpatch(body, display=opts.get('test'))

    flag = ' '.join(opts.get('flag'))
    if flag:
        flag = ' ' + flag

    subj = desc[0].strip().rstrip('. ')
    if total == 1 and not opts.get('intro'):
        subj = '[PATCH%s] %s' % (flag, opts.get('subject') or subj)
    else:
        tlen = len(str(total))
        subj = '[PATCH %0*d of %d%s] %s' % (tlen, idx, total, flag, subj)
    msg['Subject'] = mail.headencode(ui, subj, _charsets, opts.get('test'))
    msg['X-Mercurial-Node'] = node
    return msg, subj

def patchbomb(ui, repo, *revs, **opts):
    '''send changesets by email

    By default, diffs are sent in the format generated by hg export,
    one per message. The series starts with a "[PATCH 0 of N]"
    introduction, which describes the series as a whole.

    Each patch email has a Subject line of "[PATCH M of N] ...", using
    the first line of the changeset description as the subject text.
    The message contains two or three parts. First, the changeset
    description. Next, (optionally) if the diffstat program is
    installed and -d/--diffstat is used, the result of running
    diffstat on the patch. Finally, the patch itself, as generated by
    "hg export".

    By default the patch is included as text in the email body for
    easy reviewing. Using the -a/--attach option will instead create
    an attachment for the patch. With -i/--inline an inline attachment
    will be created.

    With -o/--outgoing, emails will be generated for patches not found
    in the destination repository (or only those which are ancestors
    of the specified revisions if any are provided)

    With -b/--bundle, changesets are selected as for --outgoing, but a
    single email containing a binary Mercurial bundle as an attachment
    will be sent.

    Examples::

      hg email -r 3000          # send patch 3000 only
      hg email -r 3000 -r 3001  # send patches 3000 and 3001
      hg email -r 3000:3005     # send patches 3000 through 3005
      hg email 3000             # send patch 3000 (deprecated)

      hg email -o               # send all patches not in default
      hg email -o DEST          # send all patches not in DEST
      hg email -o -r 3000       # send all ancestors of 3000 not in default
      hg email -o -r 3000 DEST  # send all ancestors of 3000 not in DEST

      hg email -b               # send bundle of all patches not in default
      hg email -b DEST          # send bundle of all patches not in DEST
      hg email -b -r 3000       # bundle of all ancestors of 3000 not in default
      hg email -b -r 3000 DEST  # bundle of all ancestors of 3000 not in DEST

    Before using this command, you will need to enable email in your
    hgrc. See the [email] section in hgrc(5) for details.
    '''

    _charsets = mail._charsets(ui)

    def outgoing(dest, revs):
        '''Return the revisions present locally but not in dest'''
        dest = ui.expandpath(dest or 'default-push', dest or 'default')
        dest, branches = hg.parseurl(dest)
        revs, checkout = hg.addbranchrevs(repo, repo, branches, revs)
        if revs:
            revs = [repo.lookup(rev) for rev in revs]
        other = hg.repository(cmdutil.remoteui(repo, opts), dest)
        ui.status(_('comparing with %s\n') % dest)
        o = repo.findoutgoing(other)
        if not o:
            ui.status(_("no changes found\n"))
            return []
        o = repo.changelog.nodesbetween(o, revs)[0]
        return [str(repo.changelog.rev(r)) for r in o]

    def getpatches(revs):
        for r in cmdutil.revrange(repo, revs):
            output = cStringIO.StringIO()
            patch.export(repo, [r], fp=output,
                         opts=patch.diffopts(ui, opts))
            yield output.getvalue().split('\n')

    def getbundle(dest):
        tmpdir = tempfile.mkdtemp(prefix='hg-email-bundle-')
        tmpfn = os.path.join(tmpdir, 'bundle')
        try:
            commands.bundle(ui, repo, tmpfn, dest, **opts)
            return open(tmpfn, 'rb').read()
        finally:
            try:
                os.unlink(tmpfn)
            except:
                pass
            os.rmdir(tmpdir)

    if not (opts.get('test') or opts.get('mbox')):
        # really sending
        mail.validateconfig(ui)

    if not (revs or opts.get('rev')
            or opts.get('outgoing') or opts.get('bundle')
            or opts.get('patches')):
        raise util.Abort(_('specify at least one changeset with -r or -o'))

    if opts.get('outgoing') and opts.get('bundle'):
        raise util.Abort(_("--outgoing mode always on with --bundle;"
                           " do not re-specify --outgoing"))

    if opts.get('outgoing') or opts.get('bundle'):
        if len(revs) > 1:
            raise util.Abort(_("too many destinations"))
        dest = revs and revs[0] or None
        revs = []

    if opts.get('rev'):
        if revs:
            raise util.Abort(_('use only one form to specify the revision'))
        revs = opts.get('rev')

    if opts.get('outgoing'):
        revs = outgoing(dest, opts.get('rev'))
    if opts.get('bundle'):
        opts['revs'] = revs

    # start
    if opts.get('date'):
        start_time = util.parsedate(opts.get('date'))
    else:
        start_time = util.makedate()

    def genmsgid(id):
        return '<%s.%s@%s>' % (id[:20], int(start_time[0]), socket.getfqdn())

    def getdescription(body, sender):
        if opts.get('desc'):
            body = open(opts.get('desc')).read()
        else:
            ui.write(_('\nWrite the introductory message for the '
                       'patch series.\n\n'))
            body = ui.edit(body, sender)
        return body

    def getpatchmsgs(patches, patchnames=None):
        jumbo = []
        msgs = []

        ui.write(_('This patch series consists of %d patches.\n\n')
                 % len(patches))

        name = None
        for i, p in enumerate(patches):
            jumbo.extend(p)
            if patchnames:
                name = patchnames[i]
            msg = makepatch(ui, repo, p, opts, _charsets, i + 1,
                            len(patches), name)
            msgs.append(msg)

        if len(patches) > 1 or opts.get('intro'):
            tlen = len(str(len(patches)))

            flag = ' '.join(opts.get('flag'))
            if flag:
                subj = '[PATCH %0*d of %d %s]' % (tlen, 0, len(patches), flag)
            else:
                subj = '[PATCH %0*d of %d]' % (tlen, 0, len(patches))
            subj += ' ' + (opts.get('subject') or
                           prompt(ui, 'Subject: ', rest=subj))

            body = ''
            if opts.get('diffstat'):
                d = cdiffstat(ui, _('Final summary:\n'), jumbo)
                if d:
                    body = '\n' + d

            body = getdescription(body, sender)
            msg = mail.mimeencode(ui, body, _charsets, opts.get('test'))
            msg['Subject'] = mail.headencode(ui, subj, _charsets,
                                             opts.get('test'))

            msgs.insert(0, (msg, subj))
        return msgs

    def getbundlemsgs(bundle):
        subj = (opts.get('subject')
                or prompt(ui, 'Subject:', 'A bundle for your repository'))

        body = getdescription('', sender)
        msg = email.MIMEMultipart.MIMEMultipart()
        if body:
            msg.attach(mail.mimeencode(ui, body, _charsets, opts.get('test')))
        datapart = email.MIMEBase.MIMEBase('application', 'x-mercurial-bundle')
        datapart.set_payload(bundle)
        bundlename = '%s.hg' % opts.get('bundlename', 'bundle')
        datapart.add_header('Content-Disposition', 'attachment',
                            filename=bundlename)
        email.Encoders.encode_base64(datapart)
        msg.attach(datapart)
        msg['Subject'] = mail.headencode(ui, subj, _charsets, opts.get('test'))
        return [(msg, subj)]

    sender = (opts.get('from') or ui.config('email', 'from') or
              ui.config('patchbomb', 'from') or
              prompt(ui, 'From', ui.username()))

    # internal option used by pbranches
    patches = opts.get('patches')
    if patches:
        msgs = getpatchmsgs(patches, opts.get('patchnames'))
    elif opts.get('bundle'):
        msgs = getbundlemsgs(getbundle(dest))
    else:
        msgs = getpatchmsgs(list(getpatches(revs)))

    def getaddrs(opt, prpt=None, default=None):
        if opts.get(opt):
            return mail.addrlistencode(ui, opts.get(opt), _charsets,
                                       opts.get('test'))

        addrs = (ui.config('email', opt) or
                 ui.config('patchbomb', opt) or '')
        if not addrs and prpt:
            addrs = prompt(ui, prpt, default)

        return mail.addrlistencode(ui, [addrs], _charsets, opts.get('test'))

    to = getaddrs('to', 'To')
    cc = getaddrs('cc', 'Cc', '')
    bcc = getaddrs('bcc')

    ui.write('\n')

    parent = opts.get('in_reply_to') or None
    # angle brackets may be omitted, they're not semantically part of the msg-id
    if parent is not None:
        if not parent.startswith('<'):
            parent = '<' + parent
        if not parent.endswith('>'):
            parent += '>'

    first = True

    sender_addr = email.Utils.parseaddr(sender)[1]
    sender = mail.addressencode(ui, sender, _charsets, opts.get('test'))
    sendmail = None
    for m, subj in msgs:
        try:
            m['Message-Id'] = genmsgid(m['X-Mercurial-Node'])
        except TypeError:
            m['Message-Id'] = genmsgid('patchbomb')
        if parent:
            m['In-Reply-To'] = parent
            m['References'] = parent
        if first:
            parent = m['Message-Id']
            first = False

        m['User-Agent'] = 'Mercurial-patchbomb/%s' % util.version()
        m['Date'] = email.Utils.formatdate(start_time[0], localtime=True)

        start_time = (start_time[0] + 1, start_time[1])
        m['From'] = sender
        m['To'] = ', '.join(to)
        if cc:
            m['Cc']  = ', '.join(cc)
        if bcc:
            m['Bcc'] = ', '.join(bcc)
        if opts.get('test'):
            ui.status(_('Displaying '), subj, ' ...\n')
            ui.flush()
            if 'PAGER' in os.environ:
                fp = util.popen(os.environ['PAGER'], 'w')
            else:
                fp = ui
            generator = email.Generator.Generator(fp, mangle_from_=False)
            try:
                generator.flatten(m, 0)
                fp.write('\n')
            except IOError, inst:
                if inst.errno != errno.EPIPE:
                    raise
            if fp is not ui:
                fp.close()
        elif opts.get('mbox'):
            ui.status(_('Writing '), subj, ' ...\n')
            fp = open(opts.get('mbox'), 'In-Reply-To' in m and 'ab+' or 'wb+')
            generator = email.Generator.Generator(fp, mangle_from_=True)
            # Should be time.asctime(), but Windows prints 2-characters day
            # of month instead of one. Make them print the same thing.
            date = time.strftime('%a %b %d %H:%M:%S %Y',
                                 time.localtime(start_time[0]))
            fp.write('From %s %s\n' % (sender_addr, date))
            generator.flatten(m, 0)
            fp.write('\n\n')
            fp.close()
        else:
            if not sendmail:
                sendmail = mail.connect(ui)
            ui.status(_('Sending '), subj, ' ...\n')
            # Exim does not remove the Bcc field
            del m['Bcc']
            fp = cStringIO.StringIO()
            generator = email.Generator.Generator(fp, mangle_from_=False)
            generator.flatten(m, 0)
            sendmail(sender, to + bcc + cc, fp.getvalue())

emailopts = [
          ('a', 'attach', None, _('send patches as attachments')),
          ('i', 'inline', None, _('send patches as inline attachments')),
          ('', 'bcc', [], _('email addresses of blind carbon copy recipients')),
          ('c', 'cc', [], _('email addresses of copy recipients')),
          ('d', 'diffstat', None, _('add diffstat output to messages')),
          ('', 'date', '', _('use the given date as the sending date')),
          ('', 'desc', '', _('use the given file as the series description')),
          ('f', 'from', '', _('email address of sender')),
          ('n', 'test', None, _('print messages that would be sent')),
          ('m', 'mbox', '',
           _('write messages to mbox file instead of sending them')),
          ('s', 'subject', '',
           _('subject of first message (intro or single patch)')),
          ('', 'in-reply-to', '',
           _('message identifier to reply to')),
          ('', 'flag', [], _('flags to add in subject prefixes')),
          ('t', 'to', [], _('email addresses of recipients')),
         ]


cmdtable = {
    "email":
        (patchbomb,
         [('g', 'git', None, _('use git extended diff format')),
          ('', 'plain', None, _('omit hg patch header')),
          ('o', 'outgoing', None,
           _('send changes not found in the target repository')),
          ('b', 'bundle', None,
           _('send changes not in target as a binary bundle')),
          ('', 'bundlename', 'bundle',
           _('name of the bundle attachment file')),
          ('r', 'rev', [], _('a revision to send')),
          ('', 'force', None,
           _('run even when remote repository is unrelated '
             '(with -b/--bundle)')),
          ('', 'base', [],
           _('a base changeset to specify instead of a destination '
             '(with -b/--bundle)')),
          ('', 'intro', None,
           _('send an introduction email for a single patch')),
         ] + emailopts + commands.remoteopts,
         _('hg email [OPTION]... [DEST]...'))
}
