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
 * Retrieves parameters related to comment ordering and display,
 * submits a GET request to /data, receives the response
 * and displays it on the page
 */
function loadComments() {
  const loginString = "/login";
  fetch(loginString)
    .then(response => response.json())
    .then(json => {
      const loggedIn = json["loggedin"];
      const url = json["url"];
      const email = json["email"]
      if (!loggedIn) {
        document.getElementById("loginlink").href = url;
        document.getElementById("loginbar").style.display = "block";
        document.getElementById("comment-sec").style.display = "none";
        return;
      }
      document.getElementById("loginbar").style.display = "none";
      document.getElementById("logout").href = url;
      document.getElementById("curremail").innerText =
        `Currently signed in as ${email}`;
      document.getElementById("comment-sec").style.display = "block";
      const isAdmin = json["isadmin"];
      if (isAdmin) {
        document.getElementById("clearcomments").className = "showclearbutton";
      } else {
        document.getElementById("clearcomments").className = "hideclearbutton";
      }
    });

  const maxcomments = document.getElementById("numcomments").value;
  const sortMetric = document.getElementById("sortby").value;
  const filterMetric = document.getElementById("filterby").value;
  const filterText = document.getElementById("filtertext").value;
  // comment language
  const lang = document.getElementById("lang").value;

  let sortOrder = "";
  if (document.getElementById("sortorder").classList.contains("desc")) {
    sortOrder = "desc";
  } else {
    sortOrder = "asc";
  }
  let fetchString = `/data?maxcomments=${maxcomments}&metric=${sortMetric}&order=${sortOrder}`;
  fetchString = fetchString + `&filterby=${filterMetric}&filtertext=${filterText}&lang=${lang}`;
  fetch(fetchString).then(response => response.json()).then(comments => {
    const commentList = document.getElementById("toplevelcomments");
    while (commentList.lastChild) {
      commentList.removeChild(commentList.lastChild);
    }
    const commentTree = locateChildren(comments);
    for (commentId in commentTree) {
      let comment = commentTree[commentId];
      if (comment["parentId"] === 0) {
        commentList.appendChild(constructReplyTree(comment, commentTree, 40));
      }
    }
    commentList.style.marginLeft = "20px";
  });
  drawChart();
}

/**
 * Converts a JSON array with indirect parent-child links to an
 * array indexed by comment ID where the object is populated
 * with an array containing the IDs of its children
 */
function locateChildren(comments) {
  let commentTree = {};
  for (let i = 0; i < comments.length; i++) {
    let id = comments[i]["id"];
    commentTree[id] = comments[i];
    commentTree[id]["children"] = [];
    for (let j = 0; j < comments.length; j++) {
      if (comments[j]["parentId"] === comments[i]["id"]) {
        commentTree[id]["children"].push(comments[j]);
      }
    }
  }
  return commentTree;
}

/**
 * Renders this comment and all of its replies in a nested
 * tree structure, indenting the replies by margin and increasing
 * the indentation by 20px for each subsequent level of replies
 */
function constructReplyTree(comment, commentTree, margin) {
  let children = commentTree[comment["id"]]["children"];
  if (children.length === 0) {
    let thisReply = createListElement(comment);
    return thisReply;
  } else {
    let thisReply = createListElement(comment);
    let replyTree = document.createElement("ul");
    replyTree.classList.add("comments", "visiblereplytree");
    const newMargin = margin + 20;
    for (const child of children) {
      const subTree = constructReplyTree(child, commentTree, newMargin);
      replyTree.appendChild(subTree);
    }
    replyTree.style.marginLeft = `${margin}px`;
    const toggleButton = createToggleButton(replyTree);
    thisReply.appendChild(toggleButton);
    thisReply.appendChild(replyTree);
    return thisReply;
  }
}

// Return button that toggles reply trees in and out
function createToggleButton(replyTree) {
  const toggleButton = document.createElement("button");
  toggleButton.classList.add("material-icons", "togglereply");
  toggleButton.innerText = "unfold_less";
  toggleButton.onclick = () => {
    if (replyTree.classList.contains("hiddenreplytree")) {
      replyTree.classList.replace("hiddenreplytree", "visiblereplytree");
      toggleButton.innerText = "unfold_less";
    } else {
      replyTree.classList.replace("visiblereplytree", "hiddenreplytree");
      toggleButton.innerText = "unfold_more";
    }
  }
  return toggleButton;
}

// Creates a list element with the given comment text and metadata (name, timestamp etc.)
function createListElement(comment) {
  const listElem = document.createElement("li");
  const metadata = formatCommentMetadata(comment);
  const quote = formatCommentText(comment);
  const reply = formatCommentReply(comment);
  const thisCommentDiv = document.createElement("div");

  thisCommentDiv.appendChild(metadata);
  formatCommentVoteButtons(comment, thisCommentDiv);
  thisCommentDiv.appendChild(quote);
  thisCommentDiv.appendChild(reply);
  if (comment["isEditable"]) {
    formatCommentEditButton(comment, thisCommentDiv, quote);
    formatCommentDeleteButton(comment, thisCommentDiv);
  }
  thisCommentDiv.className = "comment";
  listElem.appendChild(thisCommentDiv);
  return listElem;
}

/**
 * Appends a button which allows users to edit their comment when clicked
 * onto commentDiv
 */
function formatCommentEditButton(comment, commentDiv, commentText) {
  const editButton = document.createElement("button");
  editButton.className = "material-icons";
  editButton.innerText = "create";
  editButton.onclick = () => editComment(comment, commentDiv, commentText);
  commentDiv.appendChild(editButton);
}

/**
 * Triggered when edit button of a comment is clicked, opens
 * up a text area for user to format reply and a button which
 * when clicked, submits the updated comment and reloads the
 * page
 */
function editComment(comment, commentDiv, commentText) {
  const editDiv = document.createElement("div");
  editDiv.className = "editdiv";
  const editBar = document.createElement("textarea");
  editBar.innerText = commentText.innerText;
  editBar.className = "editbar";
  const editSubmit = document.createElement("button");
  editSubmit.innerText = "Submit"
  editSubmit.className = "editbutton";
  editSubmit.onclick = () => modifyComment(comment, editBar.value);
  editDiv.appendChild(editBar);
  editDiv.appendChild(editSubmit);
  while (commentDiv.firstChild) {
    commentDiv.removeChild(commentDiv.firstChild);
  }
  commentDiv.appendChild(editDiv);
}

/**
 * Submits the modified comment with content represented
 * by newContent to the server and reloads the page
 */
function modifyComment(comment, newContent) {
  const editObj = {};
  editObj["id"] = comment["id"];
  editObj["comment"] = newContent;
  fetch('/edit', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(editObj),
  }).then(response => {
    loadComments();
  });
}

/**
 * Appends a button which allows users to delete their comment when clicked
 * onto thisDiv
 */
function formatCommentDeleteButton(comment, thisDiv) {
  const deleteButton = document.createElement("button");
  deleteButton.className = "material-icons";
  deleteButton.innerText = "delete";
  deleteButton.onclick = () => deleteComment(comment);
  thisDiv.appendChild(deleteButton);
}

/**
 * Triggered when delete button of a comment is clicked, 
 * submits a request to the server to delete this comment
 * and all its replies
 */
function deleteComment(comment) {
  const deleteObj = {};
  deleteObj["id"] = comment["id"];
  fetch('/delete-one', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(deleteObj),
  }).then(response => {
    loadComments();
  });
}

/**
 * Formats upvote and downvote buttons for each comment. Sets
 * each button to pressed or unpressed based on data about
 * whether the current user has pressed either received
 * from server. 
 */
function formatCommentVoteButtons(comment, thisCommentDiv) {
  const upvoteText = document.createElement("p");
  upvoteText.className = "vote-buttons";
  upvoteText.id = `${comment["id"]}-up`;
  upvoteText.innerText = comment["upvotes"];

  /**
   * This variable can have three (String) values:
   * UPVOTED means that the current user has upvoted this comment (once)
   * DOWNVOTED means that the current user has downvoted this comment
   * NOTVOTED means that the current user has neither upvoted nor
   * downvoted this comment
   */
  const whichPressed = comment["votingStatus"];

  const upvoteButton = document.createElement("button");
  upvoteButton.classList.add("material-icons", "vote-buttons");
  upvoteButton.classList.add(whichPressed === "UPVOTED" ? "pressed" : "unpressed");
  upvoteButton.innerText = "thumb_up";
  upvoteButton.onclick =
    () => {
      if (upvoteButton.classList.contains("unpressed")) {
        /* 
         * upvote button was unpressed and user is now pressing it so we 
         * increase upvotes by 1
         */
        changeVote(comment, true, 1);
        upvoteButton.classList.replace("unpressed", "pressed");
      } else {
        /*
         * upvote button was pressed and user is now pressing it again so we 
         * decrease upvotes by 1 and undo the vote
         */
        changeVote(comment, true, -1);
        upvoteButton.classList.replace("pressed", "unpressed");
      }
    };


  const downvoteText = document.createElement("p");
  downvoteText.className = "vote-buttons";
  downvoteText.id = `${comment["id"]}-down`;
  downvoteText.innerText = comment["downvotes"];
  const downvoteButton = document.createElement("button");
  downvoteButton.classList.add("material-icons", "vote-buttons");
  downvoteButton.classList.add(whichPressed === "DOWNVOTED" ? "pressed" : "unpressed");
  downvoteButton.innerText = "thumb_down";
  downvoteButton.onclick = () => {
    if (downvoteButton.classList.contains("unpressed")) {
      /*
       * downvote button was unpressed and user is now pressing it so we 
       * increase downvotes by 1
       */
      changeVote(comment, false, 1);
      downvoteButton.classList.replace("unpressed", "pressed");
    } else {
      /*
       * downvote button was pressed and user is now pressing it again so we 
       * decrease downvotes by 1 and undo the vote
       */
      changeVote(comment, false, -1);
      downvoteButton.classList.replace("pressed", "unpressed");
    }
  };


  thisCommentDiv.appendChild(downvoteText);
  thisCommentDiv.appendChild(downvoteButton);
  thisCommentDiv.appendChild(upvoteText);
  thisCommentDiv.appendChild(upvoteButton);
}

// Formats comment name and timestamp into an HTML p element
function formatCommentMetadata(comment) {
  let date = new Date(comment["timestamp"]);
  const metadata = `${comment["name"]} (${comment["email"]}) @ ${date.toLocaleString()}:`;
  const pElem = document.createElement("p");
  pElem.innerText = metadata;
  pElem.className = "comment_metadata";
  return pElem;
}

// Formats comment text into an HTML blockquote element
function formatCommentText(comment) {
  const quote = document.createElement("blockquote");
  quote.innerText = comment["comment"];
  return quote;
}

/**
 * Creates a reply bar with a button that when clicked, submits
 * the text in the bar to the server for processing
 */
function formatCommentReply(comment) {
  const replyDiv = document.createElement("div");
  replyDiv.className = "replydiv";
  const replyBar = document.createElement("textarea");
  replyBar.className = "replybar";
  replyBar.id = `${comment["id"]}-bar`;
  const replyButton = document.createElement("button");
  replyButton.innerText = "Reply";
  replyButton.className = "replybutton";
  replyButton.onclick = () => replyTo(comment);
  replyDiv.appendChild(replyBar);
  replyDiv.appendChild(replyButton);
  return replyDiv;
}

/**
 * Triggers servlet that upvotes (if isUpvotes is true) or downvotes
 * the comment on user click. Amount must be 1 or -1. An amount of 1
 * represents an upvote if isUpvote is true and a downvote otherwise.
 * An amount of -1 represents that the current user wants to revert
 * their upvote if isUpvote is true and revert their downvote if 
 * isUpvote is false. 
 */
function changeVote(comment, isUpvote, amount) {
  // Prevent a POST request from changing vote count by more than 1
  if (amount !== 1 && amount !== -1) {
    return;
  }
  const updateObj = {};
  updateObj["id"] = comment["id"];
  updateObj["isupvote"] = isUpvote;
  updateObj["amt"] = amount;
  fetch('/update-vote', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(updateObj),
  }).then(response => {
    loadComments();
  });
}

/**
 * Triggered when reply button is clicked on a comment
 * Sends reply text along with parent comment ID to the
 * server for processing
 */
function replyTo(comment) {
  const replyId = `${comment["id"]}-bar`;
  const replyContent = document.getElementById(replyId).value;
  const replyObj = {};
  const today = new Date();
  replyObj["time"] = today.getTime();
  replyObj["comment"] = replyContent;
  replyObj["parentid"] = comment["id"];
  replyObj["rootid"] = (comment["rootId"] === 0) ? comment["id"] : comment["rootId"];
  fetch('/reply', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(replyObj),
  }).then(response => {
    loadComments();
  });
}

/**
 * Sets value of form submission time to current time in client's timezone,
 * posts form data to server, reloads comments section and then clears
 * form in preparation for next entry
 */
function submitForm(form) {
  const today = new Date();
  form.timestamp.value = today.getTime();
  const formData = {};
  const dataArray = $("#newcommentform").serializeArray();
  dataArray.forEach(entry => formData[entry.name] = entry.value);
  fetch('/data', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(formData),
  }).then(response => {
    loadComments();
    document.getElementById("newcommentform").reset();
  });
}

/**
 * Submits a post request to /delete-all to delete all comments,
 * then reloads the comments section
 */
function clearComments() {
  fetch("/delete-all", {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: ''
  }).then(response => loadComments());
}

/**
 * Toggles sort order button between ascending and descending on click
 * and then reloads comment section to effect the change
 */
function changeSortOrder() {
  const sortOrderButton = document.getElementById("sortorder");
  if (sortOrderButton.classList.contains("desc")) {
    sortOrderButton.classList.replace("desc", "asc");
    sortOrderButton.innerText = "arrow_drop_up";
  } else {
    sortOrderButton.classList.replace("asc", "desc");
    sortOrderButton.innerText = "arrow_drop_down";
  }
  loadComments();
}

/**
 *  Submits a POST request to the server informing it of a change in comment display language
 *  and then reloads the comments
 */
function changeCommentLang() {
  const langChoice = document.getElementById("lang").value;
  const langObj = {};
  langObj["lang"] = langChoice;
  fetch('/commentlang-chart', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(langObj),
  }).then(response => {
    loadComments();
  });
}