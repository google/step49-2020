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
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.EntityQuery.Builder;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;
import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.ArrayList;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/data")
public class DataServlet extends HttpServlet {

  private final String defaultMaxComment = "20";

  /*
   * Called when a client submits a GET request to the /data URL
   * Displays all recorded user comments on page
   */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    UserService userService = UserServiceFactory.getUserService();
    // Make sure user is logged in
    if (!userService.isUserLoggedIn()) {
      return;
    }
    int maxComments =
        Integer.parseInt(
            UtilityFunctions.getFieldFromResponse(request, "maxcomments", defaultMaxComment));
    String sortMetric = UtilityFunctions.getFieldFromResponse(request, "metric", "time");
    String sortOrder = UtilityFunctions.getFieldFromResponse(request, "order", "desc");
    String filterMetric = UtilityFunctions.getFieldFromResponse(request, "filterby", "comment");
    String filterText = UtilityFunctions.getFieldFromResponse(request, "filtertext", "");
    String commentLanguage = UtilityFunctions.getFieldFromResponse(request, "lang", "en");

    ArrayList<UserComment> comments = new ArrayList<>();
    populateRootComments(
        comments, maxComments, sortOrder, sortMetric, filterMetric, filterText, commentLanguage);

    Gson gson = new Gson();
    response.setContentType("application/json;charset=UTF-8");
    response.getWriter().println(gson.toJson(comments));
  }

  /*
   * Populates comments with atmost maxComments top-level queries and all their replies.
   * The top-level queries are sorted by sortMetric in sortOrder
   */
  private void populateRootComments(
      ArrayList<UserComment> comments,
      int maxComments,
      String sortOrder,
      String sortMetric,
      String filterMetric,
      String filterText,
      String langCode) {
    Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    Builder builder = Query.newEntityQueryBuilder();
    builder = builder.setKind("Comment").setFilter(PropertyFilter.eq("rootid", 0));

    if (sortOrder.equals("desc")) {
      builder = builder.setOrderBy(StructuredQuery.OrderBy.desc(sortMetric));
    } else {
      builder = builder.setOrderBy(StructuredQuery.OrderBy.asc(sortMetric));
    }

    if (filterText.length() != 0) {
      builder = builder.setFilter(PropertyFilter.eq(filterMetric, filterText));
    }

    builder = builder.setLimit(maxComments);

    Query<Entity> query = builder.build();
    QueryResults<Entity> results = datastore.run(query);
    while (results.hasNext()) {
      Entity entity = results.next();
      UserComment comment = entityToComment(entity, langCode);
      comments.add(comment);
      populateChildComments(comments, entity.getKey().getId(), langCode);
    }
  }

  // Populates comments with all replies of the comment with ID rootId
  private void populateChildComments(
      ArrayList<UserComment> comments, long rootId, String langCode) {
    Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    Builder builder = Query.newEntityQueryBuilder();
    builder = builder.setKind("Comment").setFilter(PropertyFilter.eq("rootid", rootId));

    Query<Entity> query = builder.build();
    QueryResults<Entity> results = datastore.run(query);
    while (results.hasNext()) {
      Entity entity = results.next();
      UserComment comment = entityToComment(entity, langCode);
      comments.add(comment);
    }
  }

  // Creates a UserComment object from the given entity
  private UserComment entityToComment(Entity entity, String langCode) {
    long id = entity.getKey().getId();
    String name = entity.getString("name");
    String email = entity.getString("email");
    long time = entity.getLong("time");
    String comment = entity.getString("comment");
    long parentId = entity.getLong("parentid");
    long rootId = entity.getLong("rootid");
    long upvotes = entity.getLong("upvotes");
    long downvotes = upvotes - entity.getLong("score");
    String commenterId = entity.getString("userid");

    String translatedName = name, translatedComment = comment;
    if (!langCode.equals("en")) {
      Translate translate = TranslateOptions.getDefaultInstance().getService();
      Translation nameTranslation =
          translate.translate(name, Translate.TranslateOption.targetLanguage(langCode));
      translatedName = nameTranslation.getTranslatedText();

      Translation commentTranslation =
          translate.translate(comment, Translate.TranslateOption.targetLanguage(langCode));
      translatedComment = commentTranslation.getTranslatedText();
    }

    String userId = UtilityFunctions.getCurrentUserId();
    boolean isEditable = commenterId.equals(userId);
    UserComment.voteStatus votingStatus = UserComment.voteStatus.NOTVOTED;

    /*
     * Has value:
     * 1 if user has upvoted this comment
     * -1 if user has downvoted this comment
     * 0 if user has not voted for this comment
     */
    int voteValue = UtilityFunctions.getVoteInDatastore(userId, id);
    if (voteValue != 0) {
      votingStatus =
          (voteValue == 1) ? UserComment.voteStatus.UPVOTED : UserComment.voteStatus.DOWNVOTED;
    }

    UserComment userComment =
        UserComment.create(
            translatedName,
            email,
            translatedComment,
            time,
            id,
            parentId,
            rootId,
            upvotes,
            downvotes,
            isEditable,
            votingStatus);
    return userComment;
  }

  /*
   * Called when a client submits a POST request to the /data URL
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
    String parsedBody = CharStreams.toString(request.getReader());
    JsonObject jsonComment = UtilityFunctions.stringToJsonObject(parsedBody);

    String userComment = UtilityFunctions.getFieldFromJsonObject(jsonComment, "comment", "");
    if (userComment.length() != 0) {
      String userName = UtilityFunctions.getFieldFromJsonObject(jsonComment, "name", "Anonymous");
      User currUser = userService.getCurrentUser();
      String userEmail = currUser != null ? currUser.getEmail() : "janedoe@gmail.com";
      String currDate = String.valueOf(System.currentTimeMillis());
      long userDate =
          Long.parseLong(
              UtilityFunctions.getFieldFromJsonObject(jsonComment, "timestamp", currDate));
      UtilityFunctions.addToDatastore(
          userName,
          userEmail,
          userDate,
          userComment,
          /* parentId = */ 0,
          /* rootId = */ 0,
          /* isReply = */ false,
          /* upvotes = */ 0,
          /* downvotes = */ 0);
    }
  }
}
