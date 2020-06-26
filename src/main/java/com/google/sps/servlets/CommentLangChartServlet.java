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
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/commentlang-chart")
public class CommentLangChartServlet extends HttpServlet {

  /*
   * Called when client submits a POST request to the /commentlang-chart URL
   * The request language is retrieved and the number of times it has been
   * accessed is either updated or initialized in datastore.
   */
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    UserService userService = UserServiceFactory.getUserService();
    // Make sure user is logged in
    if (!userService.isUserLoggedIn()) {
      return;
    }
    String parsedBody = CharStreams.toString(request.getReader());
    JsonObject jsonLang = UtilityFunctions.stringToJsonObject(parsedBody);

    String commentLang = UtilityFunctions.getFieldFromJsonObject(jsonLang, "lang", "en");
    UtilityFunctions.updateLangInDatastoreIfPresent(commentLang);
  }

  /*
   * Collects data about the number of times comments have been requested in various languages
   * and sends it to the client
   */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    /*
     * This data structure maps a language name to the number of times users have requested
     * comments in this language
     */
    HashMap<String, Long> numAccessesByLang = new HashMap<>();

    Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    Query<Entity> query = Query.newEntityQueryBuilder().setKind("CommentLang").build();
    QueryResults<Entity> results = datastore.run(query);

    while (results.hasNext()) {
      Entity lang = results.next();
      String langCode = lang.getString("lang");
      long numAccessInLang = lang.getLong("comments");
      Locale loc = new Locale(langCode);
      String langName = loc.getDisplayLanguage();
      numAccessesByLang.put(langName, numAccessInLang);
    }

    Gson gson = new Gson();
    response.setContentType("application/json;");
    response.getWriter().println(gson.toJson(numAccessesByLang));
  }
}
