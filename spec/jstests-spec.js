import  {initializeTippy, generateGraph, getUrl } from "../src/main/webapp/script.js";
import cytoscape from "cytoscape";

describe("Checking that fetch url is correctly constructed", function() {
  it("Input valid value for depth", function() {
    let numLayers = document.createElement("input");
    numLayers.id = "num-layers";
    numLayers.value = 15;
    document.body.appendChild(numLayers);

    let requestString = getUrl();
    let requestParams = requestString.substring(requestString.indexOf("?"));
    
    let constructedUrl = new URLSearchParams(requestParams);
    expect(constructedUrl.has("depth")).toBe(true);
    expect(constructedUrl.get("depth")).toBe("15");
  });
})


describe("Checking that tooltip is correctly initialized", function() {
  it("Node with tokens", function() {
    document.body.innerHTML = `
    <div id="cy"></div>`;
    let cy = cytoscape({
      elements: [
      ]
    });
    let nodeWithToken = {};
    nodeWithToken["data"] = {};
    nodeWithToken["data"]["id"] = "A";
    nodeWithToken["data"]["tokens"] = ["a.js", "b.js", "c.js"];
    cy.add(nodeWithToken);
    let myNode = cy.nodes()[0];
    initializeTippy(myNode);
    let content = myNode.tip.popperChildren.content.firstChild;
    expect(content.nodeName).toBe("DIV");
    expect(content.classList.contains("metadata")).toBe(true);

    let children = content.childNodes;
    expect(children.length).toBe(2);
    let closeButton =  children[0];
    expect(closeButton.nodeName).toBe("BUTTON");

    // Click on node and make sure tippy shows
    myNode.tip.show();
    expect(myNode.tip.state.isVisible).toBe(true);

    // close the tip and make sure tippy is hidden
    closeButton.click();
    expect(myNode.tip.state.isVisible).toBe(false);

    // Make assertions about tooltip content
    let tokenList = children[1];
    expect(tokenList.nodeName).toBe("UL");
    let tokens = tokenList.childNodes;
    expect(tokens.length).toBe(3);
    expect(tokens[0].nodeName).toBe("LI");
    expect(tokens[0].textContent).toBe("a.js");
    expect(tokens[1].nodeName).toBe("LI");
    expect(tokens[1].textContent).toBe("b.js");
    expect(tokens[2].nodeName).toBe("LI");
    expect(tokens[2].textContent).toBe("c.js");
  });
  it("Node without tokens", function() {
    document.body.innerHTML = `
    <div id="cy"></div>`;
    let cy = cytoscape({
      elements: [
      ]
    });
    let nodeWithoutToken = {};
    nodeWithoutToken["data"] = {};
    nodeWithoutToken["data"]["id"] = "B";
    nodeWithoutToken["data"]["tokens"] = [];
    cy.add(nodeWithoutToken);
    let myNode = cy.nodes()[0];
    initializeTippy(myNode);
    let content = myNode.tip.popperChildren.content.firstChild;
    expect(content.nodeName).toBe("DIV");
    expect(content.classList.contains("metadata")).toBe(true);

    let children = content.childNodes;
    expect(children.length).toBe(2);
    let closeButton =  children[0];
    expect(closeButton.nodeName).toBe("BUTTON");

    // Click on node and make sure tippy shows
    myNode.tip.show();
    expect(myNode.tip.state.isVisible).toBe(true);

    // close the tip and make sure tippy is hidden
    closeButton.click();
    expect(myNode.tip.state.isVisible).toBe(false);

    // Make assertions about tooltip content
    let tokenMsg = children[1];
    expect(tokenMsg.nodeName).toBe("P");
    expect(tokenMsg.textContent).toBe("No tokens");
  });
});

