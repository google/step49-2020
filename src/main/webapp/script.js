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

async function generateGraph() {
  let graphNodes = [];
  let graphEdges = [];
  const response = await fetch("/data");
  const jsonResponse = await response.json();
  let nodes = jsonResponse[0];
  let edges = jsonResponse[1];


  if (nodes && edges) {
    nodes.forEach(node =>
      graphNodes.push({
        group: "nodes",
        data: { id: node["name"] }
      }))
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
  alert("Oh no, there's no graph to display") // TODO: Handle this??
}

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
          shape: 'square',
          'background-color': 'red',
          'label': 'data(id)',
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
      grid: true,
      directed: true,
      roots: "#A, #E, #H",
      padding: 10,
      avoidOverlap: true,
    }
  });
}

