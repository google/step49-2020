import { initializeNumMutations, setCurrGraphNum, initializeTippy, generateGraph, getUrl, navigateGraph, currGraphNum, numMutations, updateButtons, highlightDiff } from "../src/main/webapp/script.js";
import cytoscape from "cytoscape";

describe("Checking that depth in fetch url is correct", function() {
  let numLayers = {};

  beforeEach(function() {
    numLayers = document.createElement("input");
    numLayers.id = "num-layers";
  });

  afterEach(function() {
    document.body.innerHTML = '';
  });

  it("accepts a large valid depth value", function() {
    numLayers.value = 15;
    document.body.appendChild(numLayers);

    const requestString = getUrl();
    const requestParams = requestString.substring(requestString.indexOf("?"));

    const constructedUrl = new URLSearchParams(requestParams);
    expect(constructedUrl.has("depth")).toBe(true);
    expect(constructedUrl.get("depth")).toBe("15");
  });

  it("accepts a small valid depth value", function() {
    numLayers.value = 2;
    document.body.appendChild(numLayers);

    const requestString = getUrl();
    const requestParams = requestString.substring(requestString.indexOf("?"));

    const constructedUrl = new URLSearchParams(requestParams);
    expect(constructedUrl.has("depth")).toBe(true);
    expect(constructedUrl.get("depth")).toBe("2");
  });

  it("fills in default value if input has no value", function() {
    document.body.appendChild(numLayers);

    const requestString = getUrl();
    const requestParams = requestString.substring(requestString.indexOf("?"));

    const constructedUrl = new URLSearchParams(requestParams);
    expect(constructedUrl.has("depth")).toBe(true);
    expect(constructedUrl.get("depth")).toBe("3");

  });

  it("rounds up negative depths to 0", function() {
    numLayers.value = -5;
    document.body.appendChild(numLayers);

    const requestString = getUrl();
    const requestParams = requestString.substring(requestString.indexOf("?"));

    const constructedUrl = new URLSearchParams(requestParams);
    expect(constructedUrl.has("depth")).toBe(true);
    expect(constructedUrl.get("depth")).toBe("0");
  });

  it("rounds down depths larger than 20 to 20", function() {
    numLayers.value = 80;
    document.body.appendChild(numLayers);

    const requestString = getUrl();
    const requestParams = requestString.substring(requestString.indexOf("?"));

    const constructedUrl = new URLSearchParams(requestParams);
    expect(constructedUrl.has("depth")).toBe(true);
    expect(constructedUrl.get("depth")).toBe("20");
  });

  it("rounds a decimal depth value to the nearest number", function() {
    numLayers.value = 2.8;
    document.body.appendChild(numLayers);

    const requestString = getUrl();
    const requestParams = requestString.substring(requestString.indexOf("?"));

    const constructedUrl = new URLSearchParams(requestParams);
    expect(constructedUrl.has("depth")).toBe(true);
    expect(constructedUrl.get("depth")).toBe("3");
  });

  it("rounds up negative decimals to 0", function() {
    numLayers.value = -2.8;
    document.body.appendChild(numLayers);

    const requestString = getUrl();
    const requestParams = requestString.substring(requestString.indexOf("?"));

    const constructedUrl = new URLSearchParams(requestParams);
    expect(constructedUrl.has("depth")).toBe(true);
    expect(constructedUrl.get("depth")).toBe("0");
  });

  it("rounds up decimals above 20 to 20", function() {
    numLayers.value = 200.8;
    document.body.appendChild(numLayers);

    const requestString = getUrl();
    const requestParams = requestString.substring(requestString.indexOf("?"));

    const constructedUrl = new URLSearchParams(requestParams);
    expect(constructedUrl.has("depth")).toBe(true);
    expect(constructedUrl.get("depth")).toBe("20");
  });
})


describe("Initializing tooltips", function() {

  it("initializes the tooltip of a node with tokens as a list of tokens", function() {
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

  it("indicates that a node without tokens has no tokens", function() {
    document.body.innerHTML = `
    <div id="cy"></div>`;
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

describe("Pressing next and previous buttons associated with a graph", function() {
  it("correctly updates mutation tracking variables and buttons on click", function() {
    initializeNumMutations(3);
    const prevButton = document.createElement("button");
    prevButton.id = "prevbutton";
    prevButton.onclick = () => { navigateGraph(-1); updateButtons(); };
    const nextButton = document.createElement("button");
    nextButton.id = "nextbutton";
    nextButton.onclick = () => { navigateGraph(1); updateButtons(); };
    document.body.appendChild(prevButton);
    document.body.appendChild(nextButton);

    expect(currGraphNum).toBe(0);
    expect(numMutations).toBe(3);

    nextButton.click();
    expect(currGraphNum).toBe(1);
    expect(nextButton.disabled).toBe(false);
    expect(prevButton.disabled).toBe(false);

    nextButton.click();
    expect(currGraphNum).toBe(2);
    expect(nextButton.disabled).toBe(false);
    expect(prevButton.disabled).toBe(false);

    nextButton.click();
    expect(currGraphNum).toBe(3);
    expect(nextButton.disabled).toBe(true);
    expect(prevButton.disabled).toBe(false);

    prevButton.click();
    expect(currGraphNum).toBe(2);
    expect(nextButton.disabled).toBe(false);
    expect(prevButton.disabled).toBe(false);

    prevButton.click();
    expect(currGraphNum).toBe(1);
    expect(nextButton.disabled).toBe(false);
    expect(prevButton.disabled).toBe(false);

    nextButton.click();
    expect(currGraphNum).toBe(2);
    expect(nextButton.disabled).toBe(false);
    expect(prevButton.disabled).toBe(false);

    prevButton.click();
    expect(currGraphNum).toBe(1);
    expect(nextButton.disabled).toBe(false);
    expect(prevButton.disabled).toBe(false);

    prevButton.click();
    expect(currGraphNum).toBe(0);
    expect(nextButton.disabled).toBe(false);
    expect(prevButton.disabled).toBe(true);

    prevButton.click();
    expect(currGraphNum).toBe(0);
    expect(nextButton.disabled).toBe(false);
    expect(prevButton.disabled).toBe(true);

    nextButton.click();
    expect(currGraphNum).toBe(1);
    expect(nextButton.disabled).toBe(false);
    expect(prevButton.disabled).toBe(false);
  });
});

describe("Check initializing variables are passed correctly", function() {
  beforeEach(function() {
    setCurrGraphNum(1);
  });

  afterEach(function() {
    setCurrGraphNum(0);
  });

  it("passes correct value of the mutations number in the fetch request", function() {
    const requestString = getUrl();
    const requestParams = requestString.substring(requestString.indexOf("?"));

    const constructedUrl = new URLSearchParams(requestParams);
    expect(constructedUrl.has("depth")).toBe(true);
    expect(constructedUrl.get("depth")).toBe("3");
    expect(constructedUrl.has("mutationNum")).toBe(true);
    expect(constructedUrl.get("mutationNum")).toBe("1");
  });
});

describe("Ensuring correct nodes are highlighted in mutated graph", function() {
  let cy;
  beforeEach(function() {
    document.body.innerHTML = `
    <div id="cy"></div>`;
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
  })
  it("highlights an added node in green", function() {
    const mutObj = {
      "type_": 1,
      "startNode_": "A"
    };
    const mutList = [];
    mutList.push(mutObj);
    highlightDiff(cy, mutList);
    // expect node to be green
    expect(cy.getElementById("A").style("background-color")).toBe('rgb(0,128,0)');
  });
  it("highlights an added edge in green", function() {
    const mutObj = {
      "type_": 2,
      "startNode_": "A",
      "endNode_": "B"
    };
    const mutList = [];
    mutList.push(mutObj);
    highlightDiff(cy, mutList);
    // expect node to be green
    expect(cy.getElementById("edgeAB").style("line-color")).toBe('rgb(0,128,0)');
    expect(cy.getElementById("edgeAB").style("target-arrow-color")).toBe('rgb(0,128,0)');
  });
  it("highlights a deleted node + edges in red", function() {
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
    // expect node and associated edges to be red and transparent
    expect(cy.getElementById("C").length).toBe(1);
    expect(cy.getElementById("C").style("background-color")).toBe("rgb(255,0,0)");
    expect(cy.getElementById("C").style("opacity")).toBe("0.25");
    expect(cy.getElementById("edgeBC").length).toBe(1);
    expect(cy.getElementById("edgeBC").style("line-color")).toBe('rgb(255,0,0)');
    expect(cy.getElementById("edgeBC").style("target-arrow-color")).toBe('rgb(255,0,0)');
    expect(cy.getElementById("edgeBC").style("opacity")).toBe('0.25');
    expect(cy.getElementById("edgeCA").style("line-color")).toBe('rgb(255,0,0)');
    expect(cy.getElementById("edgeCA").style("target-arrow-color")).toBe('rgb(255,0,0)');
    expect(cy.getElementById("edgeCA").style("opacity")).toBe('0.25');
  });
  it("highlights a changed node in yellow", function() {
    const mutObj = {
      "type_": 5,
      "startNode_": "A",
    };
    const mutList = [];
    mutList.push(mutObj);
    highlightDiff(cy, mutList);
    // expect node to be yellow
    expect(cy.getElementById("A").style("background-color")).toBe("rgb(255,255,0)");
  });
});
describe("Node search", function() {
  const cy = cytoscape({ 
    elements: [
    { data: { id: "A" } },
    { data: { id: "B" } },
    {
      data: {
        id: "AB",
        source: "A",
        target: "B"
      }
    }]
  });

  it("should be a successful search", function() {
    const result = searchNode(cy, "A");

    // search should find node
    expect(result).toBe(true);
  });

  it("should be an unsuccessful search", function() {
    let result = searchNode(cy, "C");

    // search should not find node
    expect(result).toBe(false);
  });

  it("should not search at all", function() {
    let result = searchNode(cy, "");

    // search should not find node
    expect(result).toBe(false);
  });
});

