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

package com.google.sps.servlets;

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.Entity;
import com.google.common.io.CharStreams;
import com.google.gson.JsonObject;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/update-vote")
public class VoteServlet extends HttpServlet {

  /*
   * Called when a client submits a POST request to the /update-vote URL
   * Changes the number of upvotes/downvotes of the comment based on the
   * request as well as the net score of the comment
   */
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    UserService userService = UserServiceFactory.getUserService();
    // Make sure user is logged in
    if (!userService.isUserLoggedIn()) {
      return;
    }
    String parsedBody = CharStreams.toString(request.getReader());
    JsonObject jsonVote = UtilityFunctions.stringToJsonObject(parsedBody);

    long commentId = Long.parseLong(UtilityFunctions.getFieldFromJsonObject(jsonVote, "id", "0"));
    long amount = Long.parseLong(UtilityFunctions.getFieldFromJsonObject(jsonVote, "amt", "0"));

    // Prevent a POST request from changing vote count by more than 1
    if (amount != 1 && amount != -1) {
      return;
    }

    if (commentId != 0 && amount != 0) {
      boolean isUpvote =
          Boolean.parseBoolean(
              UtilityFunctions.getFieldFromJsonObject(jsonVote, "isupvote", "true"));
      changeVoteInDatastore(commentId, isUpvote, amount);
    }
  }

  /*
   * Registers that the comment represented by commentId has been upvoted (if isUpvote
   * is true) or downvoted (if isUpvote is false) if amount is 1 and the user hasn't
   * already upvoted or downvoted the same comment. If they have, no change occurs.
   * If amount is -1 and the user has upvoted [downvoted] a comment and isUpvote is true
   * [false] then the vote is deregistered so that the user has no vote towards this comment.
   * If amount is -1 and isUpvote is true and the user has downvoted the comment, no change
   * occurs.
   */
  private void changeVoteInDatastore(long commentId, boolean isUpvote, long amount) {
    Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    KeyFactory keyFactory = datastore.newKeyFactory().setKind("Comment");
    Entity comment = datastore.get(keyFactory.newKey(commentId));

    String userId = UtilityFunctions.getCurrentUserId();
    /*
     * Possible Values:
     * 0 - current user has not voted for this comment
     * 1 - current user has upvoted this comment
     * -1 - current user has downvoted this comment
     */
    int voteValue = UtilityFunctions.getVoteInDatastore(userId, commentId);

    if (voteValue != 0 && amount == 1) {
      /*
       * The user has upvoted a comment and is trying to downvote it
       * or has downvoted the comment and is trying to upvote it. In
       * this case, no change should occur.
       */
      return;
    } else if (voteValue != 0 && amount == -1) {
      /*
       * User has upvoted/downvoted the comment and is trying to
       * revert their vote
       */
      // User has upvoted but is trying to revert downvote or vice versa (impossible)
      if ((voteValue == 1 && !isUpvote) || (voteValue == -1 && isUpvote)) {
        return;
      }
      UtilityFunctions.removeVoteInDatastore(userId, commentId);
    } else {
      // User has no vote on this comment currently and is making a fresh vote
      UtilityFunctions.addVoteToDatastore(userId, commentId, isUpvote);
    }

    long upvotes = comment.getLong("upvotes");
    long score = comment.getLong("score");
    long downvotes = upvotes - score;
    Entity updatedComment;
    if (isUpvote) {
      updatedComment =
          Entity.newBuilder(comment)
              .set("upvotes", upvotes + amount)
              .set("score", score + amount)
              .build();
    } else {
      updatedComment = Entity.newBuilder(comment).set("score", score - amount).build();
    }
    datastore.update(updatedComment);
  }
}
