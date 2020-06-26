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

google.charts.load('current', { 'packages': ['corechart'] });
google.charts.setOnLoadCallback(drawChart);


/**
 * Fetches data about the number of comments per day, the length of
 * the reply tree for all root level comments and the number of times
 * comments were accessed in various languages and displays it on 
 * the page in graphs
 */
function drawChart() {
  fetch("/numcomment-chart")
    .then(response => response.json())
    .then(numCommentsOnDay => {
      const data = new google.visualization.DataTable();
      data.addColumn("date", "Day");
      data.addColumn("number", "Comments")
      data.addColumn("number", "Replies")
      Object.keys(numCommentsOnDay).forEach((day) => {
        const rootComments = numCommentsOnDay[day]["rootComments"];
        const replies = numCommentsOnDay[day]["replies"];
        data.addRow([new Date(day), rootComments + replies, replies]);
      });
      const options = {
        "title": "Number of Comments By Day",
        "height": 400,
        "width": 525,
        "pointSize": 5,
        "vAxis": {
          "format": "0",
          "minValue": 0,
          "title": "Number of Comments"
        },
        "hAxis": {
          "format": "M/d/yy",
          "title": "Day"
        }
      };
      const chart = new google.visualization.LineChart(document.getElementById("numcommentchartdiv"));
      chart.draw(data, options)
    });
  let numRoots = 0;
  fetch("/replytree-chart")
    .then(response => response.json())
    .then(replyTreeLength => {
      numRoots = replyTreeLength.length;
      const data = new google.visualization.DataTable();
      data.addColumn("number", "Reply Tree Length");
      replyTreeLength.forEach((treeLength) => {
        data.addRow([treeLength]);
      });
      const options = {
        "title": "Length of Reply Tree",
        "height": 400,
        "width": 525,
        "vAxis": {
          "format": "0",
          "minValue": 0,
          "title": "Number of Root Comments"
        },
        "hAxis": {
          "title": "Length of Reply Tree"
        },
        "histogram": {
          "bucketSize": (numRoots < 2) ? "auto" : 2,
          "hideBucketItems": true
        }
      };
      const chart = new google.visualization.Histogram(document.getElementById("replytreechartdiv"));
      chart.draw(data, options)
    });
  fetch("/commentlang-chart")
    .then(response => response.json())
    .then(commentLangs => {
      const data = new google.visualization.DataTable();
      data.addColumn("string", "Language");
      data.addColumn("number", "Number of Requests")
      Object.keys(commentLangs).forEach((lang) => {
        data.addRow([lang, commentLangs[lang]]);
      });
      const options = {
        "title": "Number of Loads of Comments Section by Language",
        "height": 400,
        "width": 525
      };
      const chart = new google.visualization.PieChart(document.getElementById("commentlangchartdiv"));
      chart.draw(data, options)
    });
}