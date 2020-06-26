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

@WebServlet("/delete-one")
public class DeleteOneServlet extends HttpServlet {

  /*
   * Called when a POST request is submitted to /delete-one, deletes the
   * comment that was clicked as well as all of its replies
   * Invariant: The comment must have been authored by the current user
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

    if (commentId != 0) {
      deleteInDatastore(commentId);
    }
  }

  /*
   * Deletes comment represented by commentId from the datastore
   */
  private void deleteInDatastore(long commentId) {
    Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    KeyFactory keyFactory = datastore.newKeyFactory().setKind("Comment");
    Entity comment = datastore.get(keyFactory.newKey(commentId));
    String commentUserId = comment.getString("userid");
    // Make sure user is the same as the author of the comment
    if (!commentUserId.equals(UtilityFunctions.getCurrentUserId())) {
      return;
    }
    datastore.delete(keyFactory.newKey(commentId));
  }
}
