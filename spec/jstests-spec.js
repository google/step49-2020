let graphCode = require("../src/main/webapp/script");

describe("Addition", function() {
  it("The function should add 2 numbers", function() {
    var value = graphCode.AddNumber(5, 6);
    expect(value).toBe(11);
  });
});