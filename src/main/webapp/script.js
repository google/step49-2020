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
async function generateGraph() {
  // Arrays to store the cytoscape graph node and edge objects
  let graphNodes = [];
  let graphEdges = [];
  const url = `/data?depth=${1}`
  const response = await fetch(url);

  const serverErrorStatus = response.headers.get("serverError");

  // Error on server side
  if (serverErrorStatus !== null) {
    displayError(serverErrorStatus);
  }

  const jsonResponse = await response.json();
  // Graph nodes and edges received from server
  let nodes = jsonResponse[0];
  let edges = jsonResponse[1];
  let roots = jsonResponse[2];

  if (nodes && edges) {
    // Add node to array of cytoscape nodes
    nodes.forEach(node =>
      graphNodes.push({
        group: "nodes",
        data: { id: node["name"] }
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
  cytoscape({
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
          shape: 'square',
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
      name: 'breadthfirst',
      maximal: true,
      directed: true,
      padding: 10,
      avoidOverlap: true,
      spacingFactor: 2
    }
  });
}


generateGraph();
