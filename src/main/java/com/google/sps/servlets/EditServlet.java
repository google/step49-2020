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

@WebServlet("/edit")
public class EditServlet extends HttpServlet {

  /*
   * Called when a POST request is submitted to /edit, updates the comment
   * that has been edited to reflect its new content
   * Invariant: a user can only edit comments they authored
   */
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    UserService userService = UserServiceFactory.getUserService();
    // Make sure user is logged in
    if (!userService.isUserLoggedIn()) {
      return;
    }
    String parsedBody = CharStreams.toString(request.getReader());
    JsonObject jsonObject = UtilityFunctions.stringToJsonObject(parsedBody);

    long commentId = Long.parseLong(UtilityFunctions.getFieldFromJsonObject(jsonObject, "id", "0"));
    String newComment = UtilityFunctions.getFieldFromJsonObject(jsonObject, "comment", "");
    long time =
        Long.parseLong(
            UtilityFunctions.getFieldFromJsonObject(
                jsonObject, "time", String.valueOf(System.currentTimeMillis())));

    if (newComment.length() != 0) {
      editInDatastore(commentId, newComment, time);
    }
  }

  /*
   * Edits comment represented by commentId in the datastore to reflect
   * that its content is newComment. Also records its new timestamp.
   */
  private void editInDatastore(long commentId, String newComment, long time) {
    Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    KeyFactory keyFactory = datastore.newKeyFactory().setKind("Comment");
    Entity comment = datastore.get(keyFactory.newKey(commentId));
    String commentUserId = comment.getString("userid");
    // Make sure editing user is the same as the comment author
    if (!commentUserId.equals(UtilityFunctions.getCurrentUserId())) {
      return;
    }
    Entity updatedComment =
        Entity.newBuilder(comment).set("comment", newComment).set("time", time).build();
    datastore.update(updatedComment);

    // Update timestamp of this comment in datastore
    UtilityFunctions.editTimestampInDatastore(commentId, time);
  }
}
