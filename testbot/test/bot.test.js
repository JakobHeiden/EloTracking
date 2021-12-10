const { group, test, command, beforeStart, afterAll, expect } = require("corde");
// You can also import const corde = require("corde"); This is a default export with all others
// functions.

// somehow cannot put these into message strings and have them recognized as mentions
const challengerId = 697106618424492034;
const acceptorId = 918604802437558312;

group("main commands", () => {

  test("hello command should be unknown'", () => {
    expect("hello").toReturn("Unknown Command hello");
  });

  test("register a channel", () => {
    expect("register testgame").toReturn("New game created. You can now !challenge another player");
  });

  test("accept but no challenge", () => {
    expect("ac <@697106618424492034>").toReturn("That player has not yet challenged you");
  });

  test("win but no challenge", () => {
    expect("w <@918604802437558312>").toReturn("No challenge exists towards that player. Use !challenge to issue one");
  });

  test("challenge a player", () => {
    expect("ch <@918604802437558312>").toReturn("Challenge issued. Your opponent can now !accept");
  });

  test("set accepted challenge decay", () => {
    expect("setacceptedchallengedecay 9").toReturn("Accepted challenges are now set to decay after 9 minutes.");
  });

  test("set match auto resolve", () => {
    expect("setmatchautoresolve 9").toReturn("One-sided reports are now set to auto-resolve after 9 minutes.");
  });

  test("set open challenge decay", () => {
    expect("setopenchallengedecay 9").toReturn("Open challenges are now set to decay after 9 minutes.");
  });

  test("change prefix", () => {
    expect("setprefix .").toReturn("Command prefix changed to .")
  })
});
