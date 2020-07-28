
import {
  initializeNumMutations, setMutationIndexList, setCurrMutationNum, setCurrMutationIndex,
  initializeTippy, generateGraph, getUrl, navigateGraph, currMutationNum, currMutationIndex,
  numMutations, updateButtons, searchNode, highlightDiff, initializeReasonTooltip, getGraphDisplay,
  getClosestIndices, initializeSlider, resetMutationSlider, mutationNumSlider, setMutationSliderValue, 
  readGraphNumberInput, updateGraphNumInput, setMaxNumMutations, searchToken
}
  from "../src/main/webapp/script.js";

import cytoscape from "cytoscape";

describe("Checking that depth in fetch url is correct", function () {
  let numLayers = {};

  beforeEach(function () {
    numLayers = document.createElement("input");
    numLayers.id = "num-layers";
  });

  afterEach(function () {
    document.body.innerHTML = '';
  });

  it("accepts a large valid depth value", function () {
    numLayers.value = 15;
    document.body.appendChild(numLayers);

    const requestString = getUrl();
    const requestParams = requestString.substring(requestString.indexOf("?"));

    const constructedUrl = new URLSearchParams(requestParams);
    expect(constructedUrl.has("depth")).toBe(true);
    expect(constructedUrl.get("depth")).toBe("15");
  });

  it("accepts a small valid depth value", function () {
    numLayers.value = 2;
    document.body.appendChild(numLayers);

    const requestString = getUrl();
    const requestParams = requestString.substring(requestString.indexOf("?"));

    const constructedUrl = new URLSearchParams(requestParams);
    expect(constructedUrl.has("depth")).toBe(true);
    expect(constructedUrl.get("depth")).toBe("2");
  });

  it("fills in default value if input has no value", function () {
    document.body.appendChild(numLayers);

    const requestString = getUrl();
    const requestParams = requestString.substring(requestString.indexOf("?"));

    const constructedUrl = new URLSearchParams(requestParams);
    expect(constructedUrl.has("depth")).toBe(true);
    expect(constructedUrl.get("depth")).toBe("3");

  });

  it("rounds up negative depths to 0", function () {
    numLayers.value = -5;
    document.body.appendChild(numLayers);

    const requestString = getUrl();
    const requestParams = requestString.substring(requestString.indexOf("?"));

    const constructedUrl = new URLSearchParams(requestParams);
    expect(constructedUrl.has("depth")).toBe(true);
    expect(constructedUrl.get("depth")).toBe("0");
  });

  it("rounds down depths larger than 20 to 20", function () {
    numLayers.value = 80;
    document.body.appendChild(numLayers);

    const requestString = getUrl();
    const requestParams = requestString.substring(requestString.indexOf("?"));

    const constructedUrl = new URLSearchParams(requestParams);
    expect(constructedUrl.has("depth")).toBe(true);
    expect(constructedUrl.get("depth")).toBe("20");
  });

  it("rounds a decimal depth value to the nearest number", function () {
    numLayers.value = 2.8;
    document.body.appendChild(numLayers);

    const requestString = getUrl();
    const requestParams = requestString.substring(requestString.indexOf("?"));

    const constructedUrl = new URLSearchParams(requestParams);
    expect(constructedUrl.has("depth")).toBe(true);
    expect(constructedUrl.get("depth")).toBe("3");
  });

  it("rounds up negative decimals to 0", function () {
    numLayers.value = -2.8;
    document.body.appendChild(numLayers);

    const requestString = getUrl();
    const requestParams = requestString.substring(requestString.indexOf("?"));

    const constructedUrl = new URLSearchParams(requestParams);
    expect(constructedUrl.has("depth")).toBe(true);
    expect(constructedUrl.get("depth")).toBe("0");
  });

  it("rounds up decimals above 20 to 20", function () {
    numLayers.value = 200.8;
    document.body.appendChild(numLayers);

    const requestString = getUrl();
    const requestParams = requestString.substring(requestString.indexOf("?"));

    const constructedUrl = new URLSearchParams(requestParams);
    expect(constructedUrl.has("depth")).toBe(true);
    expect(constructedUrl.get("depth")).toBe("20");
  });
})


describe("Initializing tooltips", function () {
  it("initializes the tooltip of a node with tokens as a list of tokens", function () {
    document.body.innerHTML = `
    <div id="cy"></div>`;
    const cy = cytoscape({
      elements: [
      ]
    });
    const nodeWithToken = {};
    nodeWithToken["data"] = {};
    nodeWithToken["data"]["id"] = "A";
    nodeWithToken["data"]["tokens"] = ["a.js", "b.js", "c.js"];
    cy.add(nodeWithToken);
    const myNode = cy.nodes()[0];
    initializeTippy(myNode);
    const content = myNode.tip.popperChildren.content.firstChild;
    expect(content.nodeName).toBe("DIV");
    expect(content.classList.contains("metadata")).toBe(true);

    const children = content.childNodes;
    expect(children.length).toBe(2);
    const closeButton = children[0];
    expect(closeButton.nodeName).toBe("BUTTON");

    // Click on node and make sure tippy shows
    myNode.tip.show();
    expect(myNode.tip.state.isVisible).toBe(true);

    // close the tip and make sure tippy is hidden
    closeButton.click();
    expect(myNode.tip.state.isVisible).toBe(false);

    // Make assertions about tooltip content
    const tokenList = children[1];
    expect(tokenList.nodeName).toBe("UL");
    const tokens = tokenList.childNodes;
    expect(tokens.length).toBe(3);
    expect(tokens[0].nodeName).toBe("LI");
    expect(tokens[0].textContent).toBe("a.js");
    expect(tokens[1].nodeName).toBe("LI");
    expect(tokens[1].textContent).toBe("b.js");
    expect(tokens[2].nodeName).toBe("LI");
    expect(tokens[2].textContent).toBe("c.js");
  });

  it("indicates that a node without tokens has no tokens", function () {
    document.body.innerHTML = `<div id="cy"></div>`;
    const cy = cytoscape({
      elements: [
      ]
    });
    const nodeWithoutToken = {};
    nodeWithoutToken["data"] = {};
    nodeWithoutToken["data"]["id"] = "B";
    nodeWithoutToken["data"]["tokens"] = [];
    cy.add(nodeWithoutToken);
    const myNode = cy.nodes()[0];
    initializeTippy(myNode);
    const content = myNode.tip.popperChildren.content.firstChild;
    expect(content.nodeName).toBe("DIV");
    expect(content.classList.contains("metadata")).toBe(true);

    const children = content.childNodes;
    expect(children.length).toBe(2);
    const closeButton = children[0];
    expect(closeButton.nodeName).toBe("BUTTON");

    // Click on node and make sure tippy shows
    myNode.tip.show();
    expect(myNode.tip.state.isVisible).toBe(true);

    // close the tip and make sure tippy is hidden
    closeButton.click();
    expect(myNode.tip.state.isVisible).toBe(false);

    // Make assertions about tooltip content
    const tokenMsg = children[1];
    expect(tokenMsg.nodeName).toBe("P");
    expect(tokenMsg.textContent).toBe("No tokens");
  });
});

describe("Pressing next and previous buttons associated with a graph", function () {
  let numDisplay = {};
  beforeEach(function () {
    numDisplay = document.createElement("div");
    numDisplay.id = "num-mutation-display";
  }); 

  afterEach(function () {
    document.body.innerHTML = '';
  });

  it("correctly updates mutation tracking variables and button properties on click", function () {
    document.body.appendChild(numDisplay);
    initializeNumMutations(3);
    setCurrMutationNum(-1);
    setCurrMutationIndex(-1);
    // Relevant indices are different from actual indices!
    setMutationIndexList([0, 1, 3]);
    const prevButton = document.createElement("button");
    prevButton.id = "prevbutton";
    prevButton.onclick = () => { navigateGraph(-1); updateButtons(); };
    const nextButton = document.createElement("button");
    nextButton.id = "nextbutton";
    nextButton.onclick = () => { navigateGraph(1); updateButtons(); };
    document.body.appendChild(prevButton);
    document.body.appendChild(nextButton);

    expect(currMutationNum).toBe(-1);
    expect(currMutationIndex).toBe(-1);
    expect(numMutations).toBe(3);

    // Check the button properties and variables with buttons are clicked
    nextButton.click();
    expect(currMutationNum).toBe(0);
    expect(currMutationIndex).toBe(0);
    expect(nextButton.disabled).toBe(false);
    expect(prevButton.disabled).toBe(false);

    nextButton.click();
    expect(currMutationNum).toBe(1);
    expect(currMutationIndex).toBe(1);
    expect(nextButton.disabled).toBe(false);
    expect(prevButton.disabled).toBe(false);

    nextButton.click();
    expect(currMutationNum).toBe(3);
    expect(currMutationIndex).toBe(2);
    expect(nextButton.disabled).toBe(true);
    expect(prevButton.disabled).toBe(false);

    nextButton.click();
    expect(currMutationNum).toBe(3);
    expect(currMutationIndex).toBe(2);
    expect(nextButton.disabled).toBe(true);
    expect(prevButton.disabled).toBe(false);

    prevButton.click();
    expect(currMutationNum).toBe(1);
    expect(currMutationIndex).toBe(1);
    expect(nextButton.disabled).toBe(false);
    expect(prevButton.disabled).toBe(false);

    prevButton.click();
    expect(currMutationNum).toBe(0);
    expect(currMutationIndex).toBe(0);
    expect(nextButton.disabled).toBe(false);
    expect(prevButton.disabled).toBe(false);

    prevButton.click();
    expect(currMutationNum).toBe(-1);
    expect(currMutationIndex).toBe(-1);
    expect(nextButton.disabled).toBe(false);
    expect(prevButton.disabled).toBe(true);

    nextButton.click();
    expect(currMutationNum).toBe(0);
    expect(currMutationIndex).toBe(0);
    expect(nextButton.disabled).toBe(false);
    expect(prevButton.disabled).toBe(false);

    prevButton.click();
    expect(currMutationNum).toBe(-1);
    expect(currMutationIndex).toBe(-1);
    expect(nextButton.disabled).toBe(false);
    expect(prevButton.disabled).toBe(true);

    prevButton.click();
    expect(currMutationNum).toBe(-1);
    expect(currMutationIndex).toBe(-1);
    expect(nextButton.disabled).toBe(false);
    expect(prevButton.disabled).toBe(true);
  });

  it("correctly doesn't change anything when there aren't any mutations", function () {
    document.body.appendChild(numDisplay);
    initializeNumMutations(-1);
    setCurrMutationNum(10);

    // Nothing changes with a negative numMutations
    navigateGraph(-1);
    expect(currMutationNum).toBe(10);

    navigateGraph(1);
    expect(currMutationNum).toBe(10);
  });

  it("correctly navigates forward when index is a decimal", function () {
    document.body.appendChild(numDisplay);
    setCurrMutationIndex(.5);
    setCurrMutationNum(0);
    setMutationIndexList([0, 1, 3]);
    initializeNumMutations(3);
    navigateGraph(1);
    expect(currMutationIndex).toBe(1);
    expect(currMutationNum).toBe(1);
  });

  it("correctly navigates backward when index is a decimal", function () {
    document.body.appendChild(numDisplay);
    setCurrMutationIndex(.5);
    setCurrMutationNum(0);
    setMutationIndexList([0, 1, 3]);
    initializeNumMutations(3);
    navigateGraph(-1);
    expect(currMutationIndex).toBe(0);
    expect(currMutationNum).toBe(0);
  });
});

describe("Check correct url params", function () {
  let nodeName = {};
  let tokenName = {};

  beforeEach(function () {
    setCurrMutationNum(1);
    nodeName = document.createElement("input");
    nodeName.id = "node-name-filter";

    tokenName = document.createElement("input");
    tokenName.id = "token-name-filter";
  });

  afterEach(function () {
    setCurrMutationNum(0);
    document.body.innerHTML = '';
  });

  it("passes correct value of the mutations number in the fetch request", function () {
    const requestString = getUrl();
    const requestParams = requestString.substring(requestString.indexOf("?"));

    const constructedUrl = new URLSearchParams(requestParams);
    expect(constructedUrl.has("depth")).toBe(true);
    expect(constructedUrl.get("depth")).toBe("3");
    expect(constructedUrl.has("mutationNum")).toBe(true);
    expect(constructedUrl.get("mutationNum")).toBe("1");

    // Not on page here, should be empty
    expect(constructedUrl.has("nodeNames")).toBe(true);
    expect(constructedUrl.get("nodeNames")).toEqual('[""]');

    expect(constructedUrl.has("tokenName")).toBe(true);
    expect(constructedUrl.get("tokenName")).toBe("");
  });

  it("passes correct nodeName when nodeName has a value", function () {
    nodeName.value = "A";
    document.body.appendChild(nodeName); 

    const requestString = getUrl(); 
    const requestParams = requestString.substring(requestString.indexOf("?"));

    const constructedUrl = new URLSearchParams(requestParams);
    expect(constructedUrl.has("nodeNames")).toBe(true); 
    expect(constructedUrl.get("nodeNames")).toEqual('["A"]');
  });

  it("passes correct nodeName when nodeName has a comma separated value", function () {
    nodeName.value = "A,B";
    document.body.appendChild(nodeName); 

    const requestString = getUrl(); 
    const requestParams = requestString.substring(requestString.indexOf("?"));

    const constructedUrl = new URLSearchParams(requestParams);
    expect(constructedUrl.has("nodeNames")).toBe(true); 
    expect(constructedUrl.get("nodeNames")).toEqual('["A","B"]');
  });

    it("passes correct nodeName when nodeName has a comma separated value and spaces", function () {
    nodeName.value = "  Annie boo  , B";
    document.body.appendChild(nodeName);  

    const requestString = getUrl(); 
    const requestParams = requestString.substring(requestString.indexOf("?"));

    const constructedUrl = new URLSearchParams(requestParams);
    expect(constructedUrl.has("nodeNames")).toBe(true); 
    expect(constructedUrl.get("nodeNames")).toEqual('["Annie boo","B"]');
  });

  it("passes correct tokenName when tokenName has a value", function () {
    tokenName.value = "1";
    document.body.appendChild(tokenName);
 
    const requestString = getUrl();
    const requestParams = requestString.substring(requestString.indexOf("?"));
    const constructedUrl = new URLSearchParams(requestParams);
    expect(constructedUrl.has("tokenName")).toBe(true);
    expect(constructedUrl.get("tokenName")).toBe("1");
  })
});

describe("Node search", function() {
  let cy;
  let numSelected;
  let nodeError;
  let query;
  const expectedBorderWidth = "4px";
  beforeEach(function() {
    document.body.innerHTML = `<div id="cy"></div>`;

    cy = cytoscape({
    elements: [
      { data: { id: "A" } },
      { data: { id: "B" } },
      {
        data: {
          id: "edgeAB",
          source: "A",
          target: "B"
        }
      }],
      container: document.getElementById("cy"),
    });

    numSelected = document.createElement("label");
    numSelected.id = "num-selected";
    document.body.appendChild(numSelected);

    nodeError = document.createElement("label");
    nodeError.id = "node-error";
    document.body.appendChild(nodeError);

    query = document.createElement("input");
    query.id = "node-search";
    document.body.appendChild(query);
  });

  it("should be successful finding a node in the graph", function() {
    query.value = "A";
    const result = searchAndHighlight(cy, "node", searchNode);

    // should not display error message
    expect(nodeError.innerText).toBe("");
    expect(result.id()).toBe("A");
    expect(result.style("border-width")).toBe(expectedBorderWidth);
  });

  it("should be unsuccessful because node does not exist", function() {
    query.value = "C";
    const result = searchAndHighlight(cy, "node", searchNode);

    // should display error message
    expect(nodeError.innerText).toBe("node does not exist.");
    expect(result).toBeUndefined();
  });

  it("should not execute at all because there is no query", function() {
    query.value = "";
    const result = searchAndHighlight(cy, "node", searchNode);

    // should not display error message
    expect(nodeError.innerText).toBe("");
    expect(result).toBeUndefined();
  });
});

describe("Token search", function() {
  let numSelected;
  let tokenError;
  let query;
  let cy;

  beforeEach(function() {
    document.body.innerHTML = `<div id="cy"></div>`;
    cy = cytoscape({
      elements: [
      ],
      container: document.getElementById("cy"),
    });
    const nodeWithToken1 = {};
    nodeWithToken1["data"] = {};
    nodeWithToken1["data"]["id"] = "A";
    nodeWithToken1["data"]["tokens"] = ["a.js", "b.js", "c.js"];
    cy.add(nodeWithToken1);

    const nodeWithToken2 = {};
    nodeWithToken2["data"] = {};
    nodeWithToken2["data"]["id"] = "B";
    nodeWithToken2["data"]["tokens"] = ["b.js"];
    cy.add(nodeWithToken2);

    numSelected = document.createElement("label");
    numSelected.id = "num-selected";
    document.body.appendChild(numSelected);

    tokenError = document.createElement("label");
    tokenError.id = "token-error";
    document.body.appendChild(tokenError);

    query = document.createElement("input");
    query.id = "token-search";
    document.body.appendChild(query);
  });

  it("should be successful because the token exists in one node", function() {
    query.value = "a.js";
    const result = searchAndHighlight(cy, "token", searchToken);

    // error message should not be displayed
    expect(tokenError.innerText).toBe("");
    expect(result.length).toBe(1);
    expect(result[0].id()).toBe("A");
    expect(result[0].style("border-width")).toBe("4px");
  });

  it("should be successful with finding token in multiples nodes", function() {
    query.value = "b.js";
    const result = searchAndHighlight(cy, "token", searchToken);

    // error message should not be displayed
    expect(tokenError.innerText).toBe("");
    expect(result.length).toBe(2);
    expect(result[0].id()).toBe("A");
    expect(result[1].id()).toBe("B");
    expect(result[0].style("border-width")).toBe("4px");
    expect(result[1].style("border-width")).toBe("4px");
  });

  it("should be unsuccessful because token does not exist", function() {
    query.value = "fake_file.js";
    const result = searchAndHighlight(cy, "token", searchToken);

    // error message should be displayed
    expect(tokenError.innerText).toBe("token does not exist.");
    expect(result).toBeUndefined();
  });

  it("should not be executed at all because there is no query", function() {
    query.value = "";
    const result = searchAndHighlight(cy, "token", searchToken);

    // error message should not be displayed
    expect(tokenError.innerText).toBe("");
    expect(result).toBeUndefined();
  });
});

describe("Ensuring correct nodes are highlighted in mutated graph", function () {
  let cy;
  const green = "rgb(0,128,0)";
  const red = "rgb(255,0,0)";
  const yellow = "rgb(255,255,0)";
  const translucentOpacity = "0.25";

  beforeEach(function () {
    document.body.innerHTML = `<div id="cy"></div>`;
    cy = cytoscape({
      container: document.getElementById("cy"),
      elements: [{
        group: "nodes",
        data: {
          id: "A"
        }
      },
      {
        group: "nodes",
        data: {
          id: "B"
        }
      },
      {
        group: "nodes",
        data: {
          id: "edgeAB",
          source: "A",
          target: "B"
        }
      },
      ],
    });
  });

  it("highlights an added node in green", function () {
    const mutObj = {
      "type_": 1,
      "startNode_": "A"
    };
    const mutList = [];
    mutList.push(mutObj);
    highlightDiff(cy, mutList);

    expect(cy.$id("A").style("background-color")).toBe(green);
  });

  it("highlights an added edge in green", function () {
    const mutObj = {
      "type_": 2,
      "startNode_": "A",
      "endNode_": "B"
    };
    const mutList = [];
    mutList.push(mutObj);
    highlightDiff(cy, mutList);

    expect(cy.$id("edgeAB").style("line-color")).toBe(green);
    expect(cy.$id("edgeAB").style("target-arrow-color")).toBe(green);
  });

  it("highlights a deleted node + edges in red", function () {
    const deleteNode = {
      "type_": 3,
      "startNode_": "C",
    };
    const deleteEdge1 = {
      "type_": 4,
      "startNode_": "B",
      "endNode_": "C",
    };
    const deleteEdge2 = {
      "type_": 4,
      "startNode_": "C",
      "endNode_": "A",
    };
    const mutList = [];
    mutList.push(deleteNode, deleteEdge1, deleteEdge2);
    highlightDiff(cy, mutList);

    expect(cy.$id("C").length).toBe(1);
    expect(cy.$id("C").style("background-color")).toBe(red);
    expect(cy.$id("C").style("opacity")).toBe(translucentOpacity);
    expect(cy.$id("edgeBC").length).toBe(1);
    expect(cy.$id("edgeBC").style("line-color")).toBe(red);
    expect(cy.$id("edgeBC").style("target-arrow-color")).toBe(red);
    expect(cy.$id("edgeBC").style("opacity")).toBe(translucentOpacity);
    expect(cy.$id("edgeCA").style("line-color")).toBe(red);
    expect(cy.$id("edgeCA").style("target-arrow-color")).toBe(red);
    expect(cy.$id("edgeCA").style("opacity")).toBe(translucentOpacity);
  });

  it("highlights a changed node in yellow", function () {
    const mutObj = {
      "type_": 5,
      "startNode_": "A",
    };
    const mutList = [];
    mutList.push(mutObj);
    highlightDiff(cy, mutList);

    expect(cy.$id("A").style("background-color")).toBe(yellow);
  });
});

describe("Initializing mutation reason tooltips", function () {
  it("initializes the tooltip of a node without a specified reason correctly", function () {
    document.body.innerHTML = `<div id="cy"></div>`;
    const cy = cytoscape({
      elements: [
      ]
    });
    const node = {};
    node["data"] = {};
    node["data"]["id"] = "A";
    node["data"]["tokens"] = ["a.js", "b.js", "c.js"];
    cy.add(node);
    const myNode = cy.nodes()[0];
    initializeReasonTooltip(myNode);

    const content = myNode.reasonTip.popperChildren.content.firstChild;
    expect(content.nodeName).toBe("P");
    expect(content.textContent).toBe("Reason not specified");
  });

  it("initializes the tooltip of an edge with a specified reason correctly", function () {
    document.body.innerHTML = `<div id="cy"></div>`;
    const cy = cytoscape({
      elements: [
      ]
    });
    const nodeA = {};
    nodeA["data"] = {};
    nodeA["data"]["id"] = "A";
    const nodeB = {};
    nodeB["data"] = {};
    nodeB["data"]["id"] = "B";

    cy.add(nodeA);
    cy.add(nodeB);

    const edge = {};
    edge["data"] = {};
    edge["data"]["id"] = "edgeAB";
    edge["data"]["source"] = "A";
    edge["data"]["target"] = "B";
    cy.add(edge);
    const myEdge = cy.edges()[0];
    initializeReasonTooltip(myEdge, "Adding synthetic module");

    const content = myEdge.reasonTip.popperChildren.content.firstChild;
    expect(content.nodeName).toBe("P");
    expect(content.textContent).toBe("Adding synthetic module");
  });
});

describe("Showing and hiding tooltips when checkbox is clicked", function () {
  it("correctly shows/hides tooltips when checkbox is checked/unchecked", function () {
    document.body.innerHTML = `
    <div id="graph"></div>
    <button id="search-button">Search</button>
    <label id="search-error"></label>
    <input type="checkbox" id="show-mutations"></input>
    <button id="reset"></button>
    <button id="search-button"></button>
    <button id="search-token-button"></button>`;

    const nodeA = {};
    nodeA["data"] = {};
    nodeA["data"]["id"] = "A";
    const nodeB = {};
    nodeB["data"] = {};
    nodeB["data"]["id"] = "B";

    const edge = {};
    edge["data"] = {};
    edge["data"]["id"] = "edgeAB";
    edge["data"]["source"] = "A";
    edge["data"]["target"] = "B";

    let mutAddA = {};
    mutAddA["type_"] = 1;
    mutAddA["startNode_"] = "A";

    let mutAddAB = {};
    mutAddAB["type_"] = 2;
    mutAddAB["startNode_"] = "A";
    mutAddAB["endNode_"] = "B";

    const mutList = [mutAddAB, mutAddA];
    const cy = getGraphDisplay([nodeA, nodeB], [edge], mutList, "Add node A with child B");
    const showMutCheckbox = document.getElementById("show-mutations");

    // Ensure that the checkbox starts out as checked when the graph is first displayed
    expect(showMutCheckbox.checked).toBe(true);

    // and we can hover over nodes and see the reason tooltip
    const gNodeA = cy.$id("A");
    gNodeA.trigger("mouseover");
    expect(gNodeA.reasonTip.state.isVisible).toBe(true);
    gNodeA.trigger("mouseout");
    expect(gNodeA.reasonTip.state.isVisible).toBe(false);

    // unclick the checkbox
    showMutCheckbox.click();
    gNodeA.trigger("mouseover");
    // and expect tooltips to not show up anymore
    expect(gNodeA.reasonTip.state.isVisible).toBe(false);
    gNodeA.trigger("mouseout");
    expect(gNodeA.reasonTip.state.isVisible).toBe(false);
  });
});

describe("Checking binary search functions", function () {
  it("correctly returns next greater and smaller indices for an element in the list", function () {
    const arr = [1,5,7,8,11];
    const prevLower = getClosestIndices(arr, 5).lower;
    const nextHigher = getClosestIndices(arr, 5).higher;

    expect(prevLower).toBe(0);
    expect(nextHigher).toBe(2);
  });

  it("correctly returns next greater and smaller indices for an element not in the list", function () {
    const arr = [1,5,7,8,11];
    const prevLower = getClosestIndices(arr, 6).lower;
    const nextHigher = getClosestIndices(arr, 6).higher;

    expect(prevLower).toBe(1);
    expect(nextHigher).toBe(2);
  });

  it("correctly returns next greater and smaller indices for an element at the end of the list", function () {
    const arr = [1,5,7,8,11];
    const prevLower = getClosestIndices(arr, 11).lower;
    const nextHigher = getClosestIndices(arr, 11).higher;

    expect(prevLower).toBe(3);
    expect(nextHigher).toBe(5);
  });

  it("correctly returns next greater and smaller indices for an element at the start of the list", function () {
    const arr = [1,5,7,8,11];
    const prevLower = getClosestIndices(arr, 1).lower;
    const nextHigher = getClosestIndices(arr, 1).higher;

    expect(prevLower).toBe(-1);
    expect(nextHigher).toBe(1);
  });

  it("correctly returns next greater and smaller indices for an element that is larger than all list elements", function () {
    const arr = [1,5,7,8,11];
    const prevLower = getClosestIndices(arr, 12).lower;
    const nextHigher = getClosestIndices(arr, 12).higher;

    expect(prevLower).toBe(4);
    expect(nextHigher).toBe(5);
  });

  it("correctly returns next greater and smaller indices for an element that is smaller than all list elements", function () {
    const arr = [1,5,7,8,11];
    const prevLower = getClosestIndices(arr, 0).lower;
    const nextHigher = getClosestIndices(arr, 0).higher;

    expect(prevLower).toBe(-1);
    expect(nextHigher).toBe(0);
  });
});

describe("Testing slider functionality", function () {
  beforeAll(function () {
    document.body.innerHTML = '';
  });

  beforeEach(function () {
    document.body.innerHTML = `
    <div id="slider" class="mdc-slider mdc-slider--discrete mdc-slider--display-markers" tabindex="0" role="slider" aria-valuemin="0" aria-valuemax="0" aria-valuenow="0" aria-label="Select Value">
      <div class="mdc-slider__track-container">
        <div class="mdc-slider__track"></div>
        <div class="mdc-slider__track-marker-container"></div>
      </div>
      <div class="mdc-slider__thumb-container">
        <div class="mdc-slider__pin">
          <span class="mdc-slider__pin-value-marker"></span>
        </div>
        <svg class="mdc-slider__thumb" width="21" height="21">
          <circle cx="10.5" cy="10.5" r="7.875"></circle>
        </svg>
        <div class="mdc-slider__focus-ring"></div>
      </div>
    </div>
    <div id="num-mutation-display">
      <p id="graph-number-text"></p>
      <input type="number" id="graph-number"  min="0" max="0" value="0">
      <p id="total-mutation-number-text">out of 0</p>
    </div>
    `;
    initializeSlider();
  });

  it("correctly sets up the slider when the mutation index list is empty", function () {
    setMutationIndexList([]);
    initializeNumMutations(0);
    setCurrMutationNum(-1);
    setCurrMutationIndex(-1);

    resetMutationSlider();

    expect(mutationNumSlider.value).toBe(-1);
    expect(mutationNumSlider.min).toBe(-1);
    expect(mutationNumSlider.max).toBe(-1);
    expect(mutationNumSlider.step).toBe(1);

    setMutationSliderValue(7);
    expect(mutationNumSlider.value).toBe(-1);
  });

  it("correctly sets up the slider when the mutation index list is non-empty", function () {
    // Array from 0 to 109
    setMutationIndexList(Array(110).keys());
    initializeNumMutations(110);
    setCurrMutationNum(2);
    setCurrMutationIndex(2);

    resetMutationSlider();

    expect(mutationNumSlider.value).toBe(2);
    expect(mutationNumSlider.min).toBe(-1);
    expect(mutationNumSlider.max).toBe(109);
    expect(mutationNumSlider.step).toBe(2);

    setMutationSliderValue(7);
    // Snap to nearest even number because step value is 2
    expect(mutationNumSlider.value).toBe(8);

    setMutationSliderValue(-1);
    expect(mutationNumSlider.value).toBe(-1);

    setMutationSliderValue(-2);
    expect(mutationNumSlider.value).toBe(-1);
  });

  it("correctly integrates slider with next and previous buttons", function () {
    initializeNumMutations(3);
    setCurrMutationNum(1);
    setCurrMutationIndex(-0.5);
    // Relevant indices are different from actual indices!
    setMutationIndexList([2, 3, 5]);
    resetMutationSlider();
    const prevButton = document.createElement("button");
    prevButton.id = "prevbutton";
    prevButton.onclick = () => { navigateGraph(-1); updateButtons(); };
    const nextButton = document.createElement("button");
    nextButton.id = "nextbutton";
    nextButton.onclick = () => { navigateGraph(1); updateButtons(); };
    document.body.appendChild(prevButton);
    document.body.appendChild(nextButton);


    // snapped to the nearest integer
    expect(mutationNumSlider.value).toBe(0);

    nextButton.click();
    expect(mutationNumSlider.value).toBe(0);

    nextButton.click();
    expect(mutationNumSlider.value).toBe(1);

    nextButton.click();
    expect(mutationNumSlider.value).toBe(2);

    prevButton.click();
    expect(mutationNumSlider.value).toBe(1);

    prevButton.click();
    expect(mutationNumSlider.value).toBe(0);

    prevButton.click();
    expect(mutationNumSlider.value).toBe(-1);

    setMutationSliderValue(1.5);
    // Trigger change listener to update currMutationIndex
    mutationNumSlider.foundation.adapter.notifyChange();
    expect(mutationNumSlider.value).toBe(2);
    // Next button updates to correct index
    nextButton.click();
    expect(mutationNumSlider.value).toBe(2);
  });
});

describe("Testing graph input functionality", function () {
  let graphNumberInput;
  let totalMutNumberText;

  beforeEach(function () {
    document.body.innerHTML =
      `<div id="num-mutation-display">
        <p id="graph-number-text">Graph </p>
        <input type="number" id="graph-number"  min="0" max="0" value="0">
        <p id="total-mutation-number-text">out of 0</p>
      </div>`;
    graphNumberInput = document.getElementById("graph-number");
    graphNumberInput.onchange = readGraphNumberInput;

    totalMutNumberText = document.getElementById("total-mutation-number-text");
  });

  it("correctly updates graph number input when mutation list is empty", function () {
    setMutationIndexList([]);
    setCurrMutationNum(-1);
    setCurrMutationIndex(-1);
    setMaxNumMutations(0);

    updateGraphNumInput();
    // Just the initial graph
    expect(graphNumberInput.value).toBe("0");
    expect(graphNumberInput.min).toBe("0");
    expect(graphNumberInput.max).toBe("0");
    expect(totalMutNumberText.textContent).toBe("out of 0");

    // Trigger an onchange event
    graphNumberInput.value = 7;
    document.getElementById("graph-number").onchange();
    expect(graphNumberInput.value).toBe("0");
  });

  it("correctly updates graph number input when the mutation index list is non-empty", function () {
    setMutationIndexList([1, 5, 7, 11]);
    setCurrMutationNum(1);
    setCurrMutationIndex(0);
    setMaxNumMutations(20);

    updateGraphNumInput();
    // Current graph is the graph after mutation 1
    expect(graphNumberInput.value).toBe("2");
    expect(graphNumberInput.min).toBe("0");
    expect(graphNumberInput.max).toBe("20");
    expect(totalMutNumberText.textContent).toBe("out of 20");

    // Trigger an onchange event
    graphNumberInput.value = 8;
    document.getElementById("graph-number").onchange();
    expect(graphNumberInput.value).toBe("8");

    graphNumberInput.value = 30;
    document.getElementById("graph-number").onchange();
    expect(graphNumberInput.value).toBe("20");

    graphNumberInput.value = -5;
    document.getElementById("graph-number").onchange();
    expect(graphNumberInput.value).toBe("0");
  });
});