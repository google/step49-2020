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
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.json.Json;
import javax.json.JsonObjectBuilder;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

  /*
   * Called when a GET request is submitted to /login. If the user is logged in,
   * returns this status along with a logout URL. Otherwise, returns a login URL
   * and the current status to the client.
   */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("application/json");
    JsonObjectBuilder builder = Json.createObjectBuilder();
    String redirectURL = "../../comments.html";

    UserService userService = UserServiceFactory.getUserService();

    if (userService.isUserLoggedIn()) {
      boolean isAdmin = userService.isUserAdmin();
      String email = userService.getCurrentUser().getEmail();
      String logoutUrl = userService.createLogoutURL(redirectURL);
      builder =
          builder
              .add("loggedin", true)
              .add("url", logoutUrl)
              .add("email", email)
              .add("isadmin", isAdmin);
    } else {
      String loginUrl = userService.createLoginURL(redirectURL);
      builder = builder.add("loggedin", false).add("url", loginUrl);
    }
    response.getWriter().println(builder.build().toString());
  }
}
