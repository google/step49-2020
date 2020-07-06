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
import tippy from 'tippy.js';
import 'tippy.js/dist/tippy.css';


cytoscape.use(popper); // register extension
cytoscape.use(dagre); // register extension


async function generateGraph() {
  // Arrays to store the cytoscape graph node and edge objects
  let graphNodes = [];
  let graphEdges = [];
  const response = await fetch("/data");

  const serverErrorStatus = response.headers.get("serverError");

  // Error on server side
  if (serverErrorStatus !== null) {
    displayError(serverErrorStatus);
  }

  const jsonResponse = await response.json();
  // Graph nodes and edges received from server
  let nodes = jsonResponse[0];
  let edges = jsonResponse[1];

  if (nodes && edges) {
    // Add node to array of cytoscape nodes
    nodes.forEach(node =>
      graphNodes.push({
        group: "nodes",
        data: { id: node["name"], metadata: node["metadata"], tokens: node["tokenList"] },
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
  displayError("Malformed graph received from server - edges or nodes are empty");
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
          shape: 'circle',
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
    zoom: 1,
    pan: { x: 0, y: 0 },
    minZoom: .25,
    maxZoom: 2.5
  });

  // Initialize content of node's metadata popup
  cy.nodes().forEach(node => initializeTippy(node));

  // When the user clicks on a node, want to display some text in the area designated for node display
  // TODO: fix positioning, style the display, figure out which parts of the metadata is interesting to show
  cy.on('tap', 'node', function(evt) {
    const node = evt.target;
    toggleMetadata(node);
  });
}

/**
 * Initializes a tooltip containing the node's token list which is
 * toggled on click
 */
function initializeTippy(node) {
  node.addClass("show");

  let tipPosition = node.popperRef(); // used only for positioning

  // unfortunately, a dummy element must be passed as tippy only accepts a dom element as the target
  let dummyDomEle = document.createElement('div');

  node.tip = tippy(dummyDomEle, { // tippy options:
    // mandatory:
    trigger: 'manual', // call show() and hide() yourself
    lazy: false, // needed for onCreate()
    onCreate: instance => { instance.popperInstance.reference = tipPosition; }, 
    // your custom options follow:

    content: () => {
      let content = document.createElement('div');
      let nodeTokens = node.data("tokens");
      content.innerHTML = (nodeTokens.length == 0) ? "No tokens" : nodeTokens;
      content.className = "metadata";

      return content;
    }
  });
}

/**
 * Toggles the displaying of metadata where a node is positioned
 * If a node(say, A)'s metadata is showing and A is clicked again, no metadata will be displayed.
 * If A's metadata is showing and B is clicked, then B's data should be displayed instead of A's.
 */
function toggleMetadata(node) {
  if (node.hasClass("show")) {
    node.tip.show();
    node.removeClass("show");
    node.addClass("hide");
  } else {
    node.tip.hide();
    node.removeClass("hide");
    node.addClass("show");
  }
}


generateGraph();
