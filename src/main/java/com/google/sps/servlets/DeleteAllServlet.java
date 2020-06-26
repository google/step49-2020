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
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Query;
import com.google.common.collect.Iterators;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/delete-all")
public class DeleteAllServlet extends HttpServlet {

  /*
   * Called when a client submits a POST request to the /delete-data URL,
   * clears database of all comments
   */
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    UserService userService = UserServiceFactory.getUserService();
    // Make sure user is logged in and they are the website admin
    if (!userService.isUserLoggedIn() || !userService.isUserAdmin()) {
      return;
    }

    Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    Query<Key> query = Query.newKeyQueryBuilder().setKind("Comment").build();
    Key[] keys = Iterators.toArray(datastore.run(query), Key.class);
    datastore.delete(keys);
  }
}
