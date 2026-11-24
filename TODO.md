# TODO Freetalk

State as of 2026-11-25. The code is on branch `modernize` (last code commit: `d44df440`), locally and on
the fork `dagga/plugin-Freetalk`. The working copy has stayed on `master`.

## In progress: tests on the local node

Freetalk (`build0013-853-g4687`, same code as `d44df440`) is loaded from `dist/Freetalk.jar` and is stored
in `freenet.ini`.

- [ ] Log in at `http://127.0.0.1:8888/Freetalk/` and check that the identities arrive. WoT knows 4,245
      with the Freetalk context, 4,140 of them with a score of zero or more.
- [ ] See whether real old messages are still on the network (may take hours or days).
- [ ] Post a test message. **Public**: the first message adds the context "Freetalk" to the identity in WoT.
- [ ] Check posting with the normal delays (5 to 15 min or more before sending): never proven.
- [ ] Try Settings > "Download messages of authors which are not linked to the Web of Trust?". Disabled by
      default. One search per hour, the first one after a few minutes.
- [ ] Check that the Freetalk menu is shown by the fred-ng interface.
- [ ] Watch the load: about 4,140 additional USK subscriptions.

### Precautions

- `./gradlew clean` in this directory deletes `dist/`, and thereby the JAR which the node reloads at
  startup. A build from `master` would overwrite it with an older version. Copy the JAR elsewhere, or build
  in another directory.
- Do not reload WoT while Freetalk is running: it can block for up to 10 min (see lot 2).
- If WoT or Freetalk is missing after a restart of the node, see "Loading fails at startup" below.
- The database is `~/Freenet/Freetalk/Freetalk.db4o`, format 4. A JAR of `master` will refuse it.
- To go back: unload Freetalk on the Plugins page.

## Loading fails at startup

WoT or Freetalk sometimes does not load when the node starts (`NullPointerException` in db4o): 1 of 8
starts with unmodified Freetalk, too few for a reliable rate. Both share the db4o of `freenet-ext.jar`.
Nothing in this repository fixes it.

- [ ] Issue: https://github.com/dagga/fred-ng/issues/256 (plugin class loader of fred-ng: options and next
      steps).

## Does Freetalk need changes for WoT?

Unmodified Freetalk (`473222b4`) works with WoT 0.4.5 build0021 on isolated nodes. Only creating a thread by
FCP failed. Apart from that, see the loading problem above.

- [ ] Draft PR with only that fix: https://github.com/dagga/plugin-Freetalk/pull/1

## Lot 2: new FCP API (3 to 5 days)

- [ ] Replace `PluginTalkerBlocking` by `FCPPluginConnection.sendSynchronous()`. It can deliver a reply to
      the wrong caller, and waits for 10 min if WoT is reloaded (12 blocked threads, node shutdown in 102 s
      when tested).
- [ ] Move the FCP server of Freetalk from `FredPluginFCP` to `ServerSideFCPMessageHandler`.

## Lot 3: WoT event-notifications (3 to 6 weeks)

Contact xor before starting: it is the work announced in his README.

- [ ] Use the `FCPClientReferenceImplementation` of WoT, subscribe to Identities and Scores.
- [ ] Keep scores and trusts locally (currently two blocking FCP calls per displayed message).
- [ ] Measure the initial synchronization: `Subscribe` does not filter by context.
- [ ] Replace the hourly polling of WoT. In `GetIdentitiesByScore` rely on `Amount`, not on
      `Identities.Amount`, which is 1.
- Do not merge branch `event-notifications` (2011).

## Known bugs, not fixed

FCP:
- [ ] `PutMessage`: `ParentThreadID` is not documented, and `ParentID` alone creates a new thread without an
      error, although the comment in the code says that an exception is thrown.
- [ ] `PutMessage` cannot reply in a thread whose root is missing (the web interface can).
- [ ] `SubscribeToBoard` and `UnsubscribeFromBoard` send no reply on success.
- [ ] `ListThreadMessages` fails without `ThreadID`, although it is documented as optional.
- [ ] `GetMessage` with an unknown index returns an empty description.
- [ ] `FCPInterface.handle()`: `return` is missing after "Plugin is terminated" (read, not tested).

Web and display:
- [ ] GET `/ChangeTrust` and `/IntroduceIdentity` without `TaskID`: HTTP 500.
- [ ] The outbox shows the messages of all own identities.
- [ ] `Quoting.java`: a hand-written `[quote author="x"]` without `message` swallows text.

Other:
- [ ] `SubscribedBoard.mDescription` shadows `Board.mDescription`: a query on it fails.
- [ ] `WoTIdentityManager.createOwnIdentity` (with URIs) rolls back without rethrowing (read, not tested).
- [ ] NNTP port 1199 is hardcoded.
- [ ] `DatabaseClosedException` errors at node shutdown if WoT closes first (noise).
- [ ] A reply dated strictly before its parent (wrong clock) is shown before it.

Authors not linked to WoT:
- [ ] Their nickname is always "Unknown". It should be read from their WoT XML.
- [ ] They cannot be rated from Freetalk: WoT does not know them.
- [ ] The rule which refuses authors was only checked on isolated nodes: no unit test with a fake WoT. The
      same applies to adding the context with the first message.

## Not verified

- [ ] JDK 8 (none available here), Windows, macOS, Eclipse.
- [ ] GitHub CI: never run. Java 25 was added to it without checking that fred compiles there.
- [ ] Messages between a 1507 node and a fred-ng node.
- [ ] Introduction puzzles of WoT (none available during the tests).
- [ ] JDK 26 and later (db4o writes to `final` fields by reflection).

## Later

- FMS gateway or import: set aside.
- Extractor for old `Freetalk.db4o` databases: only if a backup exists.
- Replacing db4o in Freetalk: estimated at 2 to 3 months, H2 preferred. To be decided with lot 3, see the
  issue above.
- Lot 5: fewer USK subscriptions, modern FProxy pages, REST API for the fred-ng interface.
- Propose the fixes upstream, to hyphanet or xor: nothing was proposed yet.

## Isolated test nodes

Scripts and dependencies are in `~/IdeaProjects/freetalk-test-nodes` (see its `README.md`). The nodes have
to be created again: the old ones, in `/tmp`, were not kept.

## After a restart of the machine

The worktrees of the branches are in a temporary directory in `/tmp`. The commits are safe in `.git` (and
`modernize` on the fork). If `/tmp` was emptied: `git worktree prune`.
