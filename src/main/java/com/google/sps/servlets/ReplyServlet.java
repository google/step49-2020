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

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.common.io.CharStreams;
import com.google.gson.JsonObject;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/reply")
public class ReplyServlet extends HttpServlet {

  /*
   * Called when a client submits a POST request to the /reply URL
   * Adds submitted comment to internal record if the comment is
   * non-empty.
   */
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    UserService userService = UserServiceFactory.getUserService();
    // Make sure user is logged in
    if (!userService.isUserLoggedIn()) {
      return;
    }

    User currUser = userService.getCurrentUser();
    String parsedBody = CharStreams.toString(request.getReader());
    JsonObject jsonReply = UtilityFunctions.stringToJsonObject(parsedBody);

    String userComment = UtilityFunctions.getFieldFromJsonObject(jsonReply, "comment", "");
    if (userComment.length() != 0) {
      String userName = UtilityFunctions.getFieldFromJsonObject(jsonReply, "name", "Anonymous");
      String userEmail = currUser != null ? currUser.getEmail() : "janedoe@gmail.com";
      String currDate = String.valueOf(System.currentTimeMillis());
      long userDate =
          Long.parseLong(UtilityFunctions.getFieldFromJsonObject(jsonReply, "timestamp", currDate));
      long parentId =
          Long.parseLong(UtilityFunctions.getFieldFromJsonObject(jsonReply, "parentid", "0"));
      long rootId =
          Long.parseLong(UtilityFunctions.getFieldFromJsonObject(jsonReply, "rootid", "0"));
      UtilityFunctions.addToDatastore(
          userName,
          userEmail,
          userDate,
          userComment,
          parentId,
          rootId,
          /* isReply = */ true,
          /* upvotes = */ 0,
          /* downvotes = */ 0);
    }
  }
}
