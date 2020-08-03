/**
 * This file defines visual constants used in script.js
 */

// Sets the color constants for different types of nodes in the graph
const colorScheme = {
  "unmodifiedNodeColor": "blue",
  "addedObjectColor": "green",
  "deletedObjectColor": "red",
  "modifiedNodeColor": "yellow",
  "unmodifiedEdgeColor": "grey",
  "labelColor": "white", 
  "filteredNodeColor": "#FF00FF"
};

// Sets the opacity constants for different types of objects in the graph
const opacityScheme = {
  "deletedObjectOpacity": 0.25
};

// Sets the tippy popup size
const tippySize = {
  "width": 450
};

// Sets the border width constants for different types of objects in the graph
const borderScheme = {
  "queriedBorder": "5px"
}

module.exports = {
  colorScheme, 
  opacityScheme,
  tippySize,
  borderScheme,
}