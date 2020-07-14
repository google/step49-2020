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

export { initializeTippy, generateGraph, getUrl };

cytoscape.use(popper); // register extension
cytoscape.use(dagre); // register extension

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
  let nodes = JSON.parse(jsonResponse.nodes);
  let edges = JSON.parse(jsonResponse.edges);

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
    let start = edge["nodeU"]["name"];
    let end = edge["nodeV"]["name"];
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
  let selectedDepth = document.getElementById('num-layers').value;
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
  const url = `/data?depth=${selectedDepth}`
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
    console.log(node);
    node.tip.show();
  });

  document.getElementById('reset').onclick = function(){ resetNodes(cy) };

  document.getElementById('search-button').onclick = function() { search(cy, "node", searchNode, graphNodes) };
  
  document.getElementById('search-token-button').onclick = function() { search(cy, "token", searchToken, graphNodes) };
}

/**
 * Searches based on type (node or token)
 */
function search(cy, type, searchFunction, nodes) {
  resetNodes(cy);
  let errorText;
  let query = document.getElementById(type + '-search').value;
  if (query == "" || searchFunction(cy, query, nodes)) {
    errorText = "";
  } else {
    errorText = type + " does not exist.";
  }
  document.getElementById(type + '-error').innerText = errorText;
}

/**
 * Zooms in on specific node
 */
function searchNode(cy, query) {
  if (query) {
    const target = cy.$('#'+query);
    if (target.length != 0) {
      highlightNodes(cy, target);
      cy.fit(target, 50);
      return true;
    }
    return false;
  }
}

/**
 * Constructs list of nodes that contain specified token
 * and zooms in on
 */
function searchToken(cy, query, nodes) {
  let target = [];
  nodes.forEach(node => {
    if (node.data.tokens.includes(query)) {
      target.push(cy.$('#'+node.data.id));
    }
  });
  if (target.length > 0) {
    let showNode = target[0][0];
    showNode.tip.show();
    highlightNodes(cy, target);
    return true;
  }
  return false;
}

function highlightNodes(cy, target) {
  // set all nodes to background state
  cy.nodes().forEach(node => node.style('opacity', '0.25'));

  // highlight desired nodes
  target.forEach(node => {
    node.style('background-color', 'olive');
    node.style('opacity', '1');
  });
  cy.fit(target, 50);
}

function resetNodes(cy) {
  cy.nodes().forEach(node => {
    node.style('background-color', 'blue');
    node.style('opacity', '1')
  });
  cy.fit(cy.nodes(), 50);
}

/**
 * Initializes a tooltip containing the node's token list
 */
function initializeTippy(node) {
  let tipPosition = node.popperRef(); // used only for positioning

  // a dummy element must be passed as tippy only accepts a dom element as the target
  let dummyDomEle = document.createElement('div');

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
 * tokens formatted into an HTML unordered list witha  close button if
 * the node has tokens and a message indicating so if it doesn't.
 */
function getTooltipContent(node) {
  let content = document.createElement("div");

  // Create button that will close the tooltip
  let closeButton = document.createElement("button");
  closeButton.innerText = "close";
  closeButton.classList.add("material-icons", "close-button");
  closeButton.addEventListener('click', function() {
    node.tip.hide();
  }, false);
  content.appendChild(closeButton);

  let nodeTokens = node.data("tokens");
  if (nodeTokens.length === 0) {
    // The node has an empty token list
    let noTokenMsg = document.createElement("p");
    noTokenMsg.innerText = "No tokens";
    content.appendChild(noTokenMsg);
  } else {
    // The node has some tokens
    let tokenList = document.createElement("ul");
    nodeTokens.forEach(token => {
      let tokenItem = document.createElement("li");
      tokenItem.innerText = token;
      tokenList.appendChild(tokenItem);
    });
    tokenList.className = "tokenlist";
    content.appendChild(tokenList);
  }
  content.className = "metadata";

  return content;
}
