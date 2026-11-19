# D-060 delayed distant respawn fixture

This is a test fixture, not part of the shipped mod. It requires a disposable checkout,
a dedicated server, two real connected survival accounts and two distinct generated
fountains in Ink Lands. The installer is restricted to Actions. The separate workflow compiles the fixture and launches two isolated actual Minecraft clients.

Test conditions:
- use the experimental seed 42 (currently known to have a full 13-site plan);
- generate and warm two real fountains A/B, at least 256 blocks apart and at least
  256 blocks from the respective death sites; place both connected survival players far away;
- in the disposable test server only, execute
  `toi-d060-respawn <playerA> <playerB> <Ax Ay Az> <Bx By Bz>`.
- The fixture temporarily masks readiness for A's bound position only for 100 ticks.
  It injects one vanilla PERFORM_RESPAWN packet on the server per death. B must
  be replaced and alive at its own fountain before the mask is released.
- A must remain dead until release, then be replaced by the regular server retry,
  within 240 ticks. Both bindings must remain correct. The fixture never
  teleports a respawned player or issues a second test packet.
- Log `D060_RESPAWN_RESULT` is the verdict. This is a controlled server-side
  delayed-readiness test, not a network death-screen or natural unload test.
  Real-client launching, disconnect/restart variants and normal player-driven
  requests still need separate coverage.

No existing saves, artwork, default layout, player data format or production
classes are changed by this commit. Never install into the author's working copy.

## Automated two-client run
`run_clients.py` creates a disposable V3 seed 42, captures Loom's real JavaExec client configuration, launches two headless (Xvfb) actual Minecraft clients with distinct offline identities, then invokes the server fixture on two generated distant fountains. The initial respawn packets are still injected server-side; do not claim coverage of the UI or network death request. The test is not validated until the workflow produces D060_RESPAWN_SUMMARY with all checks true.

## Confirmed controlled two-client result
Run 44460826737, revision 3c6e9c8, passed all five assertions in 108 ticks. Both real clients joined and reached the two actual distant fountains. Ordinary hurt was rejected for both players; the test-only forced death precondition was used. This proves delayed server retry and isolation, not natural death, inventory retention, or the initial client network request.
