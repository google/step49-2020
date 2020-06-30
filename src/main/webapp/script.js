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









function generateGraph() {
  let graphNodes = [];
  let graphEdges = [];
  let nodes = [{"name" : "A"}, {"name" : "B"}, {"name" : "C"}];
  let edges = [{"nodeU" : {"name" : "A"}, "nodeV" : {"name" : "B"}}, {"nodeU" : {"name" : "A"}, "nodeV" : {"name" : "C"}}, {"nodeU" : {"name" : "B"}, "nodeV" : {"name" : "C"}}]
  fetch("/data").then(response => response.json()).then(jsonResponse => {
      // let nodes = jsonResponse[0];
      // let edges = jsonResponse[1];
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
      const cy = cytoscape({
      container: document.getElementById("graph"),
      elements: {
        nodes: graphNodes,
        edges: graphEdges
      }
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
        }]
    });



let options = {
    name: 'breadthfirst',
    directed: true,
    padding: 10
};
  cy.layout(options);
  });
  // cy.add({data : {id: "A"}});
  // cy.add({data : {id: "B"}});
  // cy.add({data : {id: "C"}});
  // cy.add({
  // data: {
  //   id: 'edge1',
  //   target: 'B',
  //   source: 'A'
  // }
  // });
  // cy.add({
  // data: {
  //   id: 'edge2',
  //   target: 'C',
  //   source: 'A'
  // }
  // });
  // cy.add({
  // data: {
  //   id: 'edge3',
  //   target: 'C',
  //   source: 'B'
  // }
  // });
}
    
