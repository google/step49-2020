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

export { initializeNumMutations, setCurrGraphNum, initializeTippy, generateGraph, getUrl, navigateGraph, currGraphNum, numMutations };

cytoscape.use(popper); // register extension
cytoscape.use(dagre); // register extension

// Stores the index of the graph (in sequence of mutations) currently
// displayed on the screen. Must be >= 0.
let currGraphNum = 0;
// Stores the number of mutations in the list this graph is applying
// The user cannot click next to a graph beyond this point
// currently setting to an arbitrary value for testing
let numMutations = 0;

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
  getGraphDisplay(graphNodes, graphEdges);
  return;
}

/**
 * Returns the url string given the user input
 * Ensures that the depth is an integer between 0 and 20
 */
function getUrl() {
  const depthElem = document.getElementById('num-layers');
  let selectedDepth = 0;
  if(depthElem === null) {
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
  const url = `/data?depth=${selectedDepth}&mutationNum=${currGraphNum}`;
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
 */
function getGraphDisplay(graphNodes, graphEdges) {
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
          'background-color': 'blue',
          'label': 'data(id)',
          'color': 'white',
          'font-size': '20px',
          'text-halign': 'center',
          'text-valign': 'center',
        }
      },
      {
        selector: 'edge',
        style: {
          'width': 3,
          'line-color': '#ccc',
          'target-arrow-color': '#ccc',
          'target-arrow-shape': 'triangle',
          'curve-style': 'bezier'
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
  cy.on('tap', 'node', function(evt) {
    const node = evt.target;
    node.tip.show();
  });
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
  closeButton.addEventListener('click', function() {
    node.tip.hide();
  }, false);
  content.appendChild(closeButton);

  const nodeTokens = node.data("tokens");
  if (nodeTokens.length === 0) {
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
  if(currGraphNum < 0) {
    setCurrGraphNum(0);
    return;
  }
  if(currGraphNum >= numMutations) {
    setCurrGraphNum(numMutations);
    return;
  }
  generateGraph();
  updateButtons();
}

/**
 * Updates next and previous buttons of the graph to prevent user
 * from clicking previous for the initial graph and next for the 
 * final graph
 * Assumes currGraphNum is between 0 and numMutations
 */
function updateButtons() {
  if(currGraphNum === 0) {
    document.getElementById("prevbutton").disabled = true;
  } else {
    document.getElementById("prevbutton").disabled = false;
  }
  if(currGraphNum === numMutations) {
    document.getElementById("nextbutton").disabled = true;
  } else {
    document.getElementById("nextbutton").disabled = false;
  }
}