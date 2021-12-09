const { group, test, command, beforeStart, afterAll, expect } = require("corde");
// You can also import const corde = require("corde"); This is a default export with all others
// functions.

group("main commands", () => {
  test("hello command should be unknown'", () => {
    expect("hello").toReturn("Unknown Command hello");
  });
});
