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

import { MDCSlider } from '@material/slider';
import cytoscape from 'cytoscape';
import dagre from 'cytoscape-dagre';
import popper from 'cytoscape-popper';
import tippy, { sticky } from 'tippy.js';
import 'tippy.js/dist/tippy.css';
import 'tippy.js/dist/backdrop.css';
import 'tippy.js/animations/shift-away.css';

import { colorScheme, opacityScheme, borderScheme } from './constants.js';
import "./style.scss";

export {
  initializeNumMutations, setMutationIndexList, setCurrMutationNum, setCurrMutationIndex,
  initializeTippy, generateGraph, getUrl, navigateGraph, currMutationNum, currMutationIndex,
  numMutations, updateButtons, searchAndHighlight, highlightDiff, initializeReasonTooltip, 
  getGraphDisplay, getClosestIndices, initializeSlider, resetMutationSlider, mutationNumSlider, 
  setMutationSliderValue, readGraphNumberInput, updateGraphNumInput, setMaxNumMutations, 
  searchNode, searchToken, clearLogs
};


cytoscape.use(popper); // register extension
cytoscape.use(dagre); // register extension

// Stores the index of the most recently-applied mutation in mutationIndexList
// or -1 if no mutations have been applied. The value could be a decimal, in 
// which case currMutationNum is not in the list and the variable's value
// represents the average of the indices between which currMutationNum should be
let currMutationIndex = -1;

// Stores the actual number of the most recently-applied mutation
// in the global list of mutations or -1 if no mutations have been applied
let currMutationNum = -1;

// Stores the number of mutations in mutationIndexList. The user cannot click next
// to a graph beyond this point
let numMutations = 0;

// Stores the list of indices at which the currently searched node is mutated
// (for ex, if a node was searched and the node was modified in the 1st, 4th, and 
// 5th indices, this would be [1,4,5])
let mutationIndexList = [];
// An object representing a slider whose value can be changed by the user to 
// modify currMutationIndex
let mutationNumSlider;
// The maximum number of mutations that can be applied to the initial graph
// ie. the length of the mutations list
let maxNumMutations = 0;

/**
 * Initializes the number of mutations
 */
function initializeNumMutations(num) {
  numMutations = num;
}

/** 
 * Sets the current mutation number
 */
function setCurrMutationNum(num) {
  currMutationNum = num;
}

/**
 * Sets the current mutation index
 */
function setCurrMutationIndex(num) {
  currMutationIndex = num;
}

/**
 * Sets the mutation index list
 */
function setMutationIndexList(lst) {
  mutationIndexList = lst;
}

/*
 * Sets the value of the length of the mutation list
 */
function setMaxNumMutations(num) {
  maxNumMutations = num;
}

/**
 * Submits a fetch request to the /data URL to retrieve the graph
 * and then displays it on the page
 */
async function generateGraph() {
  // Arrays to store the cytoscape graph node and edge objects
  let graphNodes = [];
  let graphEdges = [];

  // disable all possible input fields
  disableInputs();

  const url = getUrl();
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

  const queriedNodes = JSON.parse(jsonResponse.queriedNodes);

  // Set all logs to be black
  const allLogs = document.querySelectorAll('.log-msg');
  for (let i = 0; i < allLogs.length; i++) {
    allLogs[i].classList.remove('recent-log-text');
  }

  const mutList = jsonResponse["mutationDiff"].length === 0 ? null : JSON.parse(jsonResponse["mutationDiff"]);
  const reason = jsonResponse["reason"];

  mutationIndexList = JSON.parse(jsonResponse.mutationIndices);
  numMutations = mutationIndexList.length;
  maxNumMutations = jsonResponse.totalMutNumber;

  if (!nodes || !edges || !Array.isArray(nodes) || !Array.isArray(edges)) {
    displayError("Malformed graph received from server - edges or nodes are empty");
    return;
  }

  // There aren't any nodes in this graph, and there aren't any mutations pertaining to the filtered node
  if (nodes.length === 0 && numMutations === 0 && mutList.length == 0) {
    displayError("This node does not exist in any stage of the graph!");
    return;
  } else if (response.headers.get("serverMessage")) {
    // This happens if the graph doesn't contain the searched node or 
    // if the graph contains the searched node BUT it isn't mutated in this graph
    addToLogs(response.headers.get("serverMessage"));
  }
  // Update the current mutation index to reflect the new position of currMutationNumber
  // in the updated mutationIndexList between the previous smaller and the next larger
  // element.
  if (currMutationNum !== -1) {
    const indexOfNextLargerNumber = getClosestIndices(mutationIndexList, currMutationNum).higher;
    const indexOfClosestSmallerNumber = getClosestIndices(mutationIndexList, currMutationNum).lower;
    currMutationIndex = ((indexOfNextLargerNumber + indexOfClosestSmallerNumber) / 2);
  } else {
    currMutationIndex = -1;
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
  });
  getGraphDisplay(graphNodes, graphEdges, mutList, reason, queriedNodes);
  updateButtons();
  updateGraphNumInput();
  resetMutationSlider();

  mutationNumSlider.disabled = false;
  document.getElementById("graph-number").disabled = false;
}

/**
 * Disables all ways a user can change the graph number while the graph
 * is being generated
 */
function disableInputs() {
  const prevBtn = document.getElementById("prevbutton");
  const nextBtn = document.getElementById("nextbutton");
  prevBtn.disabled = true;
  nextBtn.disabled = true;
  mutationNumSlider.disabled = true;
  const graphNumInput = document.getElementById("graph-number");
  graphNumInput.disabled = true;
}

/**
 * Returns the url string given the user input
 * Ensures that the depth is an integer between 0 and 20
 */
function getUrl() {
  const depthElem = document.getElementById('num-layers');
  const nodeNames = document.getElementById('node-name-filter') ? document.getElementById('node-name-filter').value || "" : "";
  const tokenName = document.getElementById('token-name-filter') ? document.getElementById('token-name-filter').value || "" : "";
  // Takes care of "all the whitespace characters (space, tab, no-break space, etc.) 
  // and all the line terminator characters (LF, CR, etc.)" acc to documentation
  const nodeNamesArray = JSON.stringify(nodeNames.split(",").map(item => item.trim()));

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
  const url = `/data?depth=${selectedDepth}&mutationNum=${currMutationNum}&nodeNames=${nodeNamesArray}&tokenName=${tokenName}`;
  return url;
}

/**
 * Add a list element with the given message to the top of the logs list
 * @param msg the message to display in the new list element
 */
function addToLogs(msg) {
  const logsList = document.getElementById("log-list");
  const newMsg = document.createElement("li");
  newMsg.classList.add("log-msg");
  newMsg.innerText = msg;
  logsList.appendChild(newMsg);
  newMsg.classList.add("recent-log-text");
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
 * @param graphNodes a list of nodes
 * @param graphEdges a list of edges
 * @param mutList a list of mutations
 * @param reason for mutation, used for highlighting the difference
 * @param queriedNodes a list of nodes that the user had searched for
 */
function getGraphDisplay(graphNodes, graphEdges, mutList, reason, queriedNodes) {
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
          'opacity': opacityScheme["deletedObjectOpacity"]
        }
      }],
    layout: {
      name: 'dagre'
    },
    zoom: 0.75,
    pan: { x: 0, y: 0 },
    minZoom: .25,
    maxZoom: 2.5,
    textureOnViewport: true
  });

  // Initialize content of node's token list popup
  cy.nodes().forEach(node => initializeTippy(node));

  // When the user clicks on a node, display the token list tooltip for the node
  cy.on('tap', 'node', function (evt) {
    const node = evt.target;
    node.tip.show();
  });

  // Color the queried nodes (it's fuchsia because I thought it was pretty, but definitely open to change! )
  if (queriedNodes) {
    queriedNodes.forEach(nodeName => {
      cy.$id(nodeName).style('background-color', colorScheme["filteredNodeColor"]);
      cy.$id(nodeName).style('border-width', borderScheme['queriedBorder']);
    })
  }
  document.getElementById('reset').onclick = function(){ resetElements(cy) };

  document.getElementById('search-button').onclick = function() { searchAndHighlight(cy, "node", searchNode) };

  document.getElementById('search-token-button').onclick = function() { searchAndHighlight(cy, "token", searchToken) };

  document.getElementById('clear-log-btn').onclick = function () { clearLogs() };

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
        } else {
          displayError("No node called " + startNode + " in graph");
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
        } else if (!endNode) {
          displayError(endNode + " not specified");
        } else if (cy.getElementById(startNode).length === 0) {
          displayError("No node called " + startNode + " in graph");
        } else {
          displayError("No node called " + endNode + " in graph");
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
  return {
    deletedNodes,
    deletedEdges,
    addedNodes,
    addedEdges,
    modifiedNodes
  };
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
 * Calls specified search function and modifies error text if necessary
 *
 * @param cy the graph to search through
 * @param type a string representing the type of search (node or token)
 * @param searchFunction the function to run the search with
 * @returns the result of the search.
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
 * Searches and highlights/zooms a graph for specified node
 *
 * @param cy the graph to search through
 * @param query the name of the node to search for
 * @returns the result of the search.
 */
function searchNode(cy, query) {
  let target = cy.$id(query);
  if (target.length != 0) {
    cy.fit(target, 50);
    return target;
  }
}

/**
 * Searches and highlights/zooms a graph for specified token
 *
 * @param cy the graph to search through
 * @param query the name of the token to search for
 * @returns the result of the search.
 */
function searchToken(cy, query) {
  let target = cy.collection();
  cy.nodes().forEach(node => {
    // check tokens field exists since deleted nodes that are still
    // shown don't have a tokens field
    if (node.data().tokens && node.data().tokens.includes(query)) {
      target = target.add(node);
    }
  });
  if (target.length > 0) {
    return target;
  }
}

/**
 * Highlights collection of nodes/edges
 *
 * @param cy the graph that contains nodes/edges
 * @param target collection of nodes to highlight
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
 * Resets collection of nodes/edges to default state
 *
 * @param cy the graph that contains nodes/edges
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
  cy.edges().forEach(edge => {
    edge.style('line-style', 'solid');
    edge.style('z-index', '1');
  });
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
 * If currMutationIndex is decimal, next/prev would set it to the 
 * next/previous integer index (either a floor or a ceiling)
 * @param amount the amount to change the currMutationIndex by. 
 * Either 1 (for next button) or -1 (for previous button)
 */
function navigateGraph(amount) {
  // this function should not be called if there are no mutations
  if (numMutations <= 0) {
    return;
  }
  if (Number.isInteger(currMutationIndex)) {
    // In this case, currMutationNum is in mutationIndexList, so update the 
    // index by the given amount (either +1 or -1)
    currMutationIndex += amount;
  } else {
    // In this case, currMutationNum is the average of two adjacent indices
    // in mutationIndex list, so pressing next should move the index to the 
    // higher index (the ceil of the average) and pressing prev should move
    // the index to the lower index (the floor of the average)
    if (amount === 1) {
      currMutationIndex = Math.ceil(currMutationIndex);
    } else {
      currMutationIndex = Math.floor(currMutationIndex);
    }
  }
  if (currMutationIndex <= -1) {
    currMutationIndex = -1;
  }
  if (currMutationIndex >= numMutations) {
    currMutationIndex = numMutations - 1;
  }
  setMutationSliderValue(currMutationIndex);
  currMutationNum = (currMutationIndex === -1) ? -1 : mutationIndexList[currMutationIndex];
}

/**
 * Updates next and previous buttons of the graph to prevent user
 * from clicking previous for the initial graph and next for the 
 * final graph
 * Assumes currGraphNum is between 0 and numMutations
 */
function updateButtons() {
  // The use of <= and >= as opposed to === is for safety
  if (currMutationIndex <= -1) {
    document.getElementById("prevbutton").disabled = true;
  } else {
    document.getElementById("prevbutton").disabled = false;
  }
  if (currMutationIndex >= mutationIndexList.length - 1) {
    document.getElementById("nextbutton").disabled = true;
  } else {
    document.getElementById("nextbutton").disabled = false;
  }
}
 
/**
 * Get an object with the index of the element that's immediately smaller 
 * and immediately greater than the element
 * @param indicesList a list of indices, assume it's sorted
 * @param element the element to find the smaller value than
 * @return an object with the properties 'lower' and 'higher';
 * lower contains the index of the last element in indicesList smaller than
 * element or -1 if element is smaller than all elements in indicesList
 * higher contains the index of the first element in indicesList larger than
 * element or indicesList.length if element is larger than all elements
 * in indicesList
 */
function getClosestIndices(indicesList, element) {
  let start = 0; 
  let end = indicesList.length - 1;

  let toReturn = {lower: -1, higher: -1};
  let indexHigher = -1;
  while (start <= end) {
    let mid = Math.floor((start + end) / 2);
    // element is not less (greater or equal to) -> go to the right
    if (indicesList[mid] <= element) {
      indexHigher = mid + 1;
      start = mid + 1;
    }
    // go to the left otherwise, set indexHigher to the mid
    else {
      indexHigher = mid;
      end = mid - 1;
    }
  }
  toReturn['higher'] = indexHigher;
  // if indexHigher is 0, then nothing is less (so lower is -1)
  // otherwise check the previous element and either go back by 1 or 2
  toReturn['lower'] = indexHigher === 0 ? -1 : (indicesList[indexHigher - 1] < element ? indexHigher - 1 : Math.max(indexHigher - 2, -1));
  return toReturn;
}

/**
 * Initializes the mutation index slider upon document load and sets it up to
 * regenerate the graph when its value is changed
 */
function initializeSlider() {
  mutationNumSlider = new MDCSlider(document.querySelector('.mdc-slider'));
  mutationNumSlider.listen('MDCSlider:change', () => {
    currMutationIndex = Math.max(-1, mutationNumSlider.value);
    currMutationIndex = Math.min(currMutationIndex, mutationIndexList.length - 1);
    currMutationNum = (currMutationIndex === -1) ? -1 : mutationIndexList[currMutationIndex];
    generateGraph();
  });
}

/**
 * Resets the mutation index slider's current, maximum and minimum values based on 
 * currMutationIndex and numMutations. Also modifies the step value to reflect the length of 
 * mutationIndicesList (fewer steps for a large number of mutations). 
 */
function resetMutationSlider() {
  if (!mutationNumSlider) {
    return;
  }
  mutationNumSlider.min = -1;
  mutationNumSlider.max = numMutations - 1;
  mutationNumSlider.value = currMutationIndex;
  // When numMutations < 10^(k+1), step is k
  mutationNumSlider.step = Math.max(1, Math.floor(Math.log10(numMutations)));
}

/**
 * Sets the value of the mutation slider to the passed amount if it exists
 * @param amount the desired value of the mutation slider
 */
function setMutationSliderValue(amount) {
  if (!mutationNumSlider) {
    return;
  }
  mutationNumSlider.value = amount;
}

/**
 * Updates the maximum, minimum and current values of the graph number
 * input field
 */
function updateGraphNumInput() {
  const graphNumberInput = document.getElementById("graph-number");
  graphNumberInput.min = 0;
  graphNumberInput.max = maxNumMutations;
  graphNumberInput.value = currMutationNum + 1;
  const totalMutNumberText = document.getElementById("total-mutation-number-text");
  if (totalMutNumberText) {
    totalMutNumberText.innerText = `out of ${maxNumMutations}`;
  }
}

/**
 * Changes the current mutation number based on a change in the value
 * of the mutation number input
 */
function readGraphNumberInput() {
  const graphNumberInput = document.getElementById("graph-number");
  if (!graphNumberInput || graphNumberInput.length === 0) {
    return;
  }
  if (graphNumberInput.value >= maxNumMutations) {
    graphNumberInput.value = maxNumMutations;
  }
  else if (graphNumberInput.value <= 0) {
    graphNumberInput.value = 0;
  }
  currMutationNum = graphNumberInput.value - 1;
}

/**
 * Clear the log panel on the right side
 */
function clearLogs() {
  const ul = document.getElementById("log-list");
  while(ul.firstChild) {
    ul.removeChild(ul.firstChild);
  }
}