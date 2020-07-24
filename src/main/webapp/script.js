// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

/**
 * Submits a fetch request to the /data url. Upon receiving a JSON encoding of the
 * nodes and edges of the graph, renders the graph in a container on the page using
 * the cytoscape.js library
 */

import cytoscape from 'cytoscape';
import dagre from 'cytoscape-dagre';
import popper from 'cytoscape-popper';
import tippy, { sticky } from 'tippy.js';
import 'tippy.js/dist/tippy.css';
import 'tippy.js/dist/backdrop.css';
import 'tippy.js/animations/shift-away.css';

export {
  searchNode, initializeNumMutations, setCurrGraphNum, initializeTippy,
  generateGraph, getUrl, navigateGraph, currGraphNum, numMutations, updateButtons,
  highlightDiff, initializeReasonTooltip, getGraphDisplay
};


cytoscape.use(popper); // register extension
cytoscape.use(dagre); // register extension

// Stores the index of the graph (in sequence of mutations) currently
// displayed on the screen. Must be >= 0.
let currGraphNum = 0;
// Stores the number of mutations in the list this graph is applying
// The user cannot click next to a graph beyond this point
let numMutations = 0;
// An object containing key-value pairs of various types of graph
// objects and their custom colors
const colorScheme = {
  "unmodifiedNodeColor": "blue",
  "addedObjectColor": "green",
  "deletedObjectColor": "red",
  "modifiedNodeColor": "yellow",
  "unmodifiedEdgeColor": "grey",
  "labelColor": "white"
};
// Sets the opacity constants for different types of objects in the graph
const opacityScheme = {
  "deletedObjectOpacity": 0.25
};

/**
 * Initializes the number of mutations
 */
function initializeNumMutations(num) {
  numMutations = num;
}

/** 
 * Sets the current graph number
 */
function setCurrGraphNum(num) {
  currGraphNum = num;
}

/**
 * Submits a fetch request to the /data URL to retrieve the graph
 * and then displays it on the page
 */
async function generateGraph() {
  // Arrays to store the cytoscape graph node and edge objects
  let graphNodes = [];
  let graphEdges = [];

  // disable buttons
  const prevBtn = document.getElementById("prevbutton");
  const nextBtn = document.getElementById("nextbutton");
  prevBtn.disabled = true;
  nextBtn.disabled = true;

  const url = getUrl();

  prevBtn.disabled = true;
  nextBtn.disabled = true;

  const response = await fetch(url);

  const serverErrorStatus = response.headers.get("serverError");

  // Error on server side
  if (serverErrorStatus !== null) {
    displayError(serverErrorStatus);
    return;
  }

  const jsonResponse = await response.json();
  // Graph nodes and edges received from server
  const nodes = JSON.parse(jsonResponse.nodes);
  const edges = JSON.parse(jsonResponse.edges);
  initializeNumMutations(JSON.parse(jsonResponse.numMutations));
  const mutList = jsonResponse["mutationDiff"].length === 0 ? null : JSON.parse(jsonResponse["mutationDiff"]);
  const reason = jsonResponse["reason"];

  if (!nodes || !edges || !Array.isArray(nodes) || !Array.isArray(edges)) {
    displayError("Malformed graph received from server - edges or nodes are empty");
    return;
  }

  if (nodes.length === 0) {
    displayError("Nothing to display!");
    return;
  }

  // Add node to array of cytoscape nodes
  nodes.forEach(node =>
    graphNodes.push({
      group: "nodes",
      data: { id: node["name"], metadata: node["metadata"], tokens: node["tokenList"] }
    }))
  // and edge to array of cytoscape edges
  edges.forEach(edge => {
    const start = edge["nodeU"]["name"];
    const end = edge["nodeV"]["name"];
    graphEdges.push({
      group: "edges",
      data: {
        id: `edge${start}${end}`,
        target: end,
        source: start
      }
    });
  })
  getGraphDisplay(graphNodes, graphEdges, mutList, reason);
  updateButtons();
  return;
}

/**
 * Returns the url string given the user input
 * Ensures that the depth is an integer between 0 and 20
 */
function getUrl() {
  const depthElem = document.getElementById('num-layers');
  const nodeName = document.getElementById('node-name') ? document.getElementById('node-name').value || "" : ""; 

  let selectedDepth = 0;
  if (depthElem === null) {
    selectedDepth = 3;
  }
  else {
    selectedDepth = depthElem.value
    if (selectedDepth.length === 0) {
      selectedDepth = 3;
    } else if (!Number.isInteger(selectedDepth)) {
      selectedDepth = Math.round(selectedDepth);
    }
    if (selectedDepth < 0) { // Extra validation for bounds
      selectedDepth = 0;
    } else if (selectedDepth > 20) {
      selectedDepth = 20;
    }
  }
  const url = `/data?depth=${selectedDepth}&mutationNum=${currGraphNum}&nodeName=${nodeName}`;
  return url;
}
/**
 * Takes an error message and creates a text element on the page to display this message
 */
function displayError(errorMsg) {
  // Create text to display the error
  const errorText = document.createElement("p");
  errorText.innerText = errorMsg;
  errorText.id = "errortext";

  const graphDiv = document.getElementById("graph");
  while (graphDiv.lastChild) {
    graphDiv.removeChild(graphDiv.lastChild);
  }
  graphDiv.appendChild(errorText);
  return;
}

/**
 * Takes in graph nodes and edges and creates a cytoscape graph with this
 * data. Assumes that the graph is a DAG to display it in the optimal layout.
 * Returns the cytoscape graph object.
 */
function getGraphDisplay(graphNodes, graphEdges, mutList, reason) {
  const cy = cytoscape({
    container: document.getElementById("graph"),
    elements: {
      nodes: graphNodes,
      edges: graphEdges
    },
    style: [
      {
        selector: 'node',
        style: {
          width: '50px',
          height: '50px',
          'background-color': colorScheme["unmodifiedNodeColor"],
          'label': 'data(id)',
          'color': colorScheme["labelColor"],
          'font-size': '20px',
          'text-halign': 'center',
          'text-valign': 'center',
        }
      },
      {
        selector: 'edge',
        style: {
          'width': 3,
          'line-color': colorScheme["unmodifiedEdgeColor"],
          'target-arrow-color': colorScheme["unmodifiedEdgeColor"],
          'target-arrow-shape': 'triangle',
          'curve-style': 'bezier'
        },
      },
      {
        selector: '.non-highlighted',
        style: {
          'opacity': '0.25'
        }
      }],
    layout: {
      name: 'dagre'
    },
    zoom: 0.75,
    pan: { x: 0, y: 0 },
    minZoom: .25,
    maxZoom: 2.5
  });

  // Initialize content of node's token list popup
  cy.nodes().forEach(node => initializeTippy(node));

  // When the user clicks on a node, display the token list tooltip for the node
  cy.on('tap', 'node', function (evt) {
    const node = evt.target;
    node.tip.show();
  });

  document.getElementById('reset').onclick = function(){ resetElements(cy) };

  document.getElementById('search-button').onclick = function() { searchAndHighlight(cy, "node", searchNode) };

  document.getElementById('search-token-button').onclick = function() { searchAndHighlight(cy, "token", searchToken) };

  // When a new graph is loaded, mutations are always shown by default
  const showMutButton = document.getElementById("show-mutations");
  showMutButton.checked = true;

  // Highlight and retrieve the objects that will be modified by this mutation
  const result = highlightDiff(cy, mutList, reason);
  if (Object.keys(result).length === 0) {
    return;
  }

  let elems = cy.collection();
  Object.entries(result).forEach(([, value]) => { elems = elems.union(value); })

  // Reposition added elements
  cy.layout({
    name: 'dagre'
  }).run();

  // Zoom in on them and activate their reason tooltips
  makeInteractiveAndFocus(cy, elems);

  
  // Break the list down into individual constituents
  const deletedNodes = result["deletedNodes"] || cy.collection();
  const deletedEdges = result["deletedEdges"] || cy.collection();
  const addedNodes = result["addedNodes"] || cy.collection();
  const addedEdges = result["addedEdges"] || cy.collection();
  const modifiedNodes = result["modifiedNodes"] || cy.collection();

  showMutButton.addEventListener("change", () => {
    if (showMutButton.checked) {
      /*
       * We take advantage of the efficiency of batch operations to avoid calling
       * highlightDiff again each time the checkbox is clicked
       */
      showDiffs(cy, elems, deletedNodes, deletedEdges, addedNodes, addedEdges, modifiedNodes);
      // Activate tooltips and zoom in on mutated objects
      makeInteractiveAndFocus(cy, elems);
    } else {
      hideDiffs(cy, elems, deletedNodes, deletedEdges, addedNodes, addedEdges, modifiedNodes);
    }
  });
  return cy;
}


/**
 * Highlights modified nodes and edges in the graph according to the list
 * of mutations
 * 
 * @param cy the graph 
 * @param mutList the list of mutations to highlight
 * @param reason the reason for the mutations
 * @returns an object containing the deleted nodes, deleted edges, added
 * nodes, added edges and modified nodes as per the mutationList or an empty
 * object if there are no mutations
 */
function highlightDiff(cy, mutList, reason = "") {
  // If the mutation list is null/undefined
  if (!mutList) {
    return {};
  }

  // Initialize empty collections
  let deletedNodes = cy.collection();
  let deletedEdges = cy.collection();
  let addedNodes = cy.collection();
  let addedEdges = cy.collection();
  let modifiedNodes = cy.collection();

  // Apply each mutation
  mutList.forEach(mutation => {
    const type = mutation["type_"] || -1;
    const startNode = mutation["startNode_"];
    const endNode = mutation["endNode_"];
    let modifiedObj = cy.collection();

    if (!type || !startNode) {
      return;
    }

    switch (type) {
      case 1:
        // add node
        if (cy.$id(startNode).length !== 0) {
          modifiedObj = cy.$id(startNode);
          // color this node green
          modifiedObj.style('background-color', colorScheme["addedObjectColor"]);
          addedNodes = addedNodes.union(modifiedObj);
        }
        break;
      case 2:
        // add edge
        if (endNode && cy.$id(startNode).length !== 0 && cy.$id(endNode).length !== 0) {
          modifiedObj = cy.$id(`edge${startNode}${endNode}`);
          // color this edge green
          modifiedObj.style('line-color', colorScheme["addedObjectColor"]);
          modifiedObj.style('target-arrow-color', colorScheme["addedObjectColor"]);
          addedEdges = addedEdges.union(modifiedObj);
        }
        break;
      case 3:
        // delete node
        // add a phantom node (if it doesn't already exist) and color it red
        if (cy.$id(startNode).length === 0) {
          cy.add({
            group: "nodes",
            data: { id: startNode }
          });
        }
        modifiedObj = cy.$id(startNode);
        modifiedObj.style('background-color', colorScheme["deletedObjectColor"]);
        modifiedObj.style('opacity', opacityScheme["deletedObjectOpacity"]);
        deletedNodes = deletedNodes.union(modifiedObj);
        break;
      case 4:
        // delete edge
        if (!endNode) {
          break;
        }
        // if corresponding nodes don't exist, add them
        if (cy.$id(startNode).length === 0) {
          cy.add({
            group: "nodes",
            data: { id: startNode }
          });
        }
        if (cy.$id(endNode).length === 0) {
          cy.add({
            group: "nodes",
            data: { id: endNode }
          });
        }
        // Add a phantom edge and color it red
        cy.add({
          group: "edges",
          data: {
            id: `edge${startNode}${endNode}`,
            target: endNode,
            source: startNode
          }
        });
        modifiedObj = cy.$id(`edge${startNode}${endNode}`);
        modifiedObj.style('line-color', colorScheme["deletedObjectColor"]);
        modifiedObj.style('target-arrow-color', colorScheme["deletedObjectColor"]);
        modifiedObj.style('opacity', opacityScheme["deletedObjectOpacity"]);
        deletedEdges = deletedEdges.union(modifiedObj);
        break;
      case 5:
        // change node
        if (cy.$id(startNode).length !== 0) {
          modifiedObj = cy.$id(startNode);
          modifiedObj.style('background-color', colorScheme["modifiedNodeColor"]);
          modifiedNodes = modifiedNodes.union(modifiedObj);
        }
        break;
      default:
        break;
    }
    if (modifiedObj.length !== 0) {
      initializeReasonTooltip(modifiedObj, reason);
    }
  });
  const returnObject = {
    "deletedNodes": deletedNodes,
    "deletedEdges": deletedEdges,
    "addedNodes": addedNodes,
    "addedEdges": addedEdges,
    "modifiedNodes": modifiedNodes
  }
  return returnObject;
}


/**
 * Initializes a tooltip with reason as its contents that displays when the object
 * is hovered over
 * @param obj the cytoscape object to display the tooltip over when hovered
 * @param reason the reason for the mutation
 */
function initializeReasonTooltip(obj, reason) {
  const tipPosition = obj.popperRef(); // used only for positioning

  // a dummy element must be passed as tippy only accepts a dom element as the target
  const dummyDomEle = document.createElement('div');

  obj.reasonTip = tippy(dummyDomEle, {
    trigger: 'manual',
    lazy: false,
    onCreate: instance => { instance.popperInstance.reference = tipPosition; },

    content: () => {
      let text = document.createElement("p");
      text.innerText = (!reason || reason.length === 0) ? "Reason not specified" : reason;
      return text;
    },
    sticky: true,
    plugins: [sticky]
  });
}

/**
 * Shows the mutations made to this graph by highlighting them
 * 
 * @param cy the graph to modify
 * @param elems all the elements to mutate
 * @param deletedNodes the nodes which were deleted to get this graph (red)
 * @param deletedEdges the edges which were deleted to get this graph (red)
 * @param addedNodes the nodes which were added to get this graph (green)
 * @param addedEdges the edges which were added to get this graph (green)
 * @param modifiedNodes the nodes which were modified to get this graph (yellow)
 */
function showDiffs(cy, elems, deletedNodes, deletedEdges, addedNodes, addedEdges, modifiedNodes) {
  // Add phantom nodes and edges to represent deleted objects
  cy.add(deletedNodes);
  cy.add(deletedEdges);

  // Color "deleted" nodes and edges in red 
  deletedNodes.style("background-color", colorScheme["deletedObjectColor"]);
  deletedEdges.style("line-color", colorScheme["deletedObjectColor"]);
  deletedEdges.style("target-arrow-color", colorScheme["deletedObjectColor"]);

  // Color "added" nodes and edges in green
  addedNodes.style("background-color", colorScheme["addedObjectColor"]);
  addedEdges.style("line-color", colorScheme["addedObjectColor"]);
  addedEdges.style("target-arrow-color", colorScheme["addedObjectColor"]);

  // Color nodes whose metadata changed in yellow
  modifiedNodes.style("background-color", colorScheme["modifiedNodeColor"]);
}

/**
 * Activates tooltips that open on hovering over objects in elems and then zooms 
 * in on these elements if possible
 * @param cy the graph to modify
 * @param elems the elements for which tooltips should be shown on mouseover
 */
function makeInteractiveAndFocus(cy, elems) {
  // Add listeners to show and hide tooltips
  elems.on('mouseover', function (evt) {
    if (evt.target.reasonTip) {
      evt.target.reasonTip.show();
    }
  });
  elems.on('mouseout', function (evt) {
    if (evt.target.reasonTip) {
      evt.target.reasonTip.hide();
    }
  });
  cy.fit(elems);
}

/**
 * Reverts the highlighted mutations on the graph, displaying only the base graph
 * 
 * @param cy the graph to modify
 * @param elems all the elements that were mutated
 * @param deletedNodes the nodes which were deleted to get this graph 
 * @param deletedEdges the edges which were deleted to get this graph 
 * @param addedNodes the nodes which were added to get this graph 
 * @param addedEdges the edges which were added to get this graph 
 * @param modifiedNodes the nodes which were modified to get this graph 
 */
function hideDiffs(cy, elems, deletedNodes, deletedEdges, addedNodes, addedEdges, modifiedNodes) {
  // Remove phantom nodes and edges
  cy.remove(deletedEdges);
  cy.remove(deletedNodes);

  // Reset the color of "added" and modified" nodes and edges 
  addedNodes.style("background-color", colorScheme["unmodifiedNodeColor"]);
  modifiedNodes.style("background-color", colorScheme["unmodifiedNodeColor"]);
  addedEdges.style("line-color", colorScheme["unmodifiedEdgeColor"]);
  addedEdges.style("target-arrow-color", colorScheme["unmodifiedEdgeColor"]);

  // Remove event listeners for tooltip manipulation
  elems.removeListener('mouseover');
  elems.removeListener('mouseout');

  // zoom out
  cy.fit();
}

/**
 * Searches a cytoscape graph (cy) based on type (node or token)
 * using a specified search function.
 * Returns the result of the search.
 */
function searchAndHighlight(cy, type, searchFunction) {
  resetElements(cy);
  let errorText = "";
  const query = document.getElementById(type + '-search').value;
  let result;
  if (query !== "") {
    result = searchFunction(cy, query);
    if (result) {
      highlightElements(cy, result);
    } else {
      errorText = type + " does not exist.";
    }
  }
  document.getElementById(type + '-error').innerText = errorText;
  return result;
}

/**
 * Finds specific node from query and zooms in on it.
 * Returns node if it exists
 */
function searchNode(cy, query) {
  let target = cy.$id(query);
  if (target.length != 0) {
    cy.fit(target, 50);
    return target;
  }
}
/**
 * Constructs list of nodes that contain specified token
 * and zooms in.
 * Returns list of nodes that contain token
 */
function searchToken(cy, query) {
  let target = cy.collection();
  cy.nodes().forEach(node => {
    if (node.data().tokens.includes(query)) {
      target = target.add(node);
    }
  });
  if (target.length > 0) {
    const showNode = target[0][0];
    showNode.tip.show();
    return target;
  }
}

/**
 * Highlights collection of nodes and edges
 */
function highlightElements(cy, target) {
  cy.nodes().forEach(node => node.toggleClass('non-highlighted', true));

  // highlight desired nodes
  target.forEach(node => {
    node.style('border-width', '4px');
    node.toggleClass('non-highlighted', false);
  });
  cy.fit(target[0], 50);
  document.getElementById('num-selected').innerText = "Number of nodes selected: " + target.length;

  // highlight adjacent edges
  target.connectedEdges().forEach(edge => {
    edge.style('line-style', 'dashed');
    edge.style('z-index', '2');
  });
}

/**
 * Resets highlighted elements to default state
 */
function resetElements(cy) {
  // reset node borders and opacity
  cy.nodes().forEach(node => {
    node.style('border-width', '0px');
    // only change opacity of nodes that were changed
    // because of highlighting (leave nodes changed due to
    // mutation alone)
    if (node.hasClass('non-highlighted')) {
      node.toggleClass('non-highlighted', false);
    }
  });

  // reset edge style
  cy.edges().forEach(edge => edge.style('line-style', 'solid'));
  document.getElementById('num-selected').innerText = "Number of nodes selected: 0";
}

/**
 * Initializes a tooltip containing the node's token list
 */
function initializeTippy(node) {
  const tipPosition = node.popperRef(); // used only for positioning

  // a dummy element must be passed as tippy only accepts a dom element as the target
  const dummyDomEle = document.createElement('div');

  node.tip = tippy(dummyDomEle, {
    trigger: 'manual',
    lazy: false,
    onCreate: instance => { instance.popperInstance.reference = tipPosition; },

    content: () => getTooltipContent(node),
    interactive: true,
    appendTo: document.body,
    // the tooltip  adheres to the node if the graph is zoomed in on
    sticky: true,
    plugins: [sticky]
  });
}

/**
 * Takes in a node and returns an HTML element containing the element's
 * tokens formatted into an HTML unordered list with a close button if
 * the node has tokens and a message indicating so if it doesn't.
 */
function getTooltipContent(node) {
  const content = document.createElement("div");

  // Create button that will close the tooltip
  const closeButton = document.createElement("button");
  closeButton.innerText = "close";
  closeButton.classList.add("material-icons", "close-button");
  closeButton.addEventListener('click', function () {
    node.tip.hide();
  }, false);
  content.appendChild(closeButton);

  const nodeTokens = node.data("tokens");
  if (!nodeTokens || nodeTokens.length === 0) {
    // The node has an empty token list
    const noTokenMsg = document.createElement("p");
    noTokenMsg.innerText = "No tokens";
    content.appendChild(noTokenMsg);
  } else {
    // The node has some tokens
    const tokenList = document.createElement("ul");
    nodeTokens.forEach(token => {
      const tokenItem = document.createElement("li");
      tokenItem.innerText = token;
      tokenList.appendChild(tokenItem);
    });
    tokenList.className = "tokenlist";
    content.appendChild(tokenList);
  }
  content.className = "metadata";

  return content;
}

/**
 * When a next/previous button is clicked, modifies the mutation index of the
 * current graph to represent the new state. Then, the corresponding
 * graph is requested from the server.
 */
function navigateGraph(amount) {
  currGraphNum += amount;
  if (currGraphNum <= 0) {
    currGraphNum = 0;
  }
  if (currGraphNum >= numMutations) {
    currGraphNum = numMutations;
  }
}

/**
 * Updates next and previous buttons of the graph to prevent user
 * from clicking previous for the initial graph and next for the 
 * final graph
 * Assumes currGraphNum is between 0 and numMutations
 */
function updateButtons() {
  if (currGraphNum === 0) {
    document.getElementById("prevbutton").disabled = true;
  } else {
    document.getElementById("prevbutton").disabled = false;
  }
  if (currGraphNum === numMutations) {
    document.getElementById("nextbutton").disabled = true;
  } else {
    document.getElementById("nextbutton").disabled = false;
  }
}