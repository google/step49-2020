
let cytoscape = require('cytoscape');
let getContent = require('./src/main/webapp/script');
let assert = require('assert');

test('Tests tooltip contents', () => {
  document.body.innerHTML = `
    <div id="cy"></div>`;
  let cy = cytoscape({
  elements: [
  ]});
  let nodeWithToken = {};
  nodeWithToken["data"] = {};
  nodeWithToken["data"]["id"] = "A";
  nodeWithToken["data"]["tokens"] = ["a.js", "b.js", "c.js"];
  cy.add(nodeWithToken);
  let myNode = cy.nodes()[0];
  let content = getContent(myNode);
  let tokenList = "<ul class=\"tokenlist\"><li>a.js</li><li>b.js</li><li>c.js</li></ul>";
  assert.equal(tokenList, content.innerHTML);
});