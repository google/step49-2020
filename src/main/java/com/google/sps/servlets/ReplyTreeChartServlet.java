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

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.common.collect.Iterators;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/replytree-chart")
public class ReplyTreeChartServlet extends HttpServlet {

  /*
   * Called when a client submits a POST request to the /replytree-chart URL
   * Prepares data about the length of the reply tree for each top-level comment
   * and sends it to the client for rendering
   */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // Maps each root comment text to the length of that comment's reply tree
    ArrayList<Integer> replyTreeSize = new ArrayList<>();

    Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    Query<Entity> query =
        Query.newEntityQueryBuilder()
            .setKind("Comment")
            .setFilter(PropertyFilter.eq("rootid", 0))
            .build();
    QueryResults<Entity> results = datastore.run(query);

    while (results.hasNext()) {
      Entity comment = results.next();
      long rootId = comment.getKey().getId();

      Query<Entity> childQuery =
          Query.newEntityQueryBuilder()
              .setKind("Comment")
              .setFilter(PropertyFilter.eq("rootid", rootId))
              .build();
      Iterator<Entity> childResults = datastore.run(childQuery);
      String text = comment.getString("comment");
      replyTreeSize.add(Iterators.size(childResults));
    }

    Gson gson = new Gson();
    response.setContentType("application/json;");
    response.getWriter().println(gson.toJson(replyTreeSize));
  }
}
