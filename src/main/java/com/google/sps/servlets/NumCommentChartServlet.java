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
import com.google.gson.Gson;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/numcomment-chart")
public class NumCommentChartServlet extends HttpServlet {

  /*
   * Called when a client submits a POST request to the /numcomment-chart URL
   * Prepares data about the number of comments each day and submits
   * it to the client for rendering
   */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

    /*
     * This data structure maps a date string in the format (06-31-2020) to a DayComments object
     * with 2 attributes - the number of root comments on this day, and the number of replies
     * on this day
     */
    HashMap<String, DayComments> numCommentsOnDay = new HashMap<>();

    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    Query<Entity> query = Query.newEntityQueryBuilder().setKind("DateEntry").build();
    QueryResults<Entity> results = datastore.run(query);

    while (results.hasNext()) {
      Entity comment = results.next();

      long dateTime = comment.getLong("time");
      Date date = new Date(dateTime);
      String dateString = dateFormat.format(date);

      DayComments prevEntry;
      if (numCommentsOnDay.containsKey(dateString)) {
        prevEntry = numCommentsOnDay.get(dateString);
      } else {
        prevEntry = DayComments.create(0, 0);
      }

      DayComments thisDayComments;
      long rootId = comment.getLong("rootid");
      if (rootId == 0) {
        // If this comment is a root comment, increase the number of root comments today by one
        thisDayComments = DayComments.create(prevEntry.rootComments() + 1, prevEntry.replies());
      } else {
        // Otherwise, increase the number of replies today by one
        thisDayComments = DayComments.create(prevEntry.rootComments(), prevEntry.replies() + 1);
      }
      numCommentsOnDay.put(dateString, thisDayComments);
    }

    Gson gson = new Gson();
    response.setContentType("application/json;");
    response.getWriter().println(gson.toJson(numCommentsOnDay));
  }
}
