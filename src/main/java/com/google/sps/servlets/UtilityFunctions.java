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
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.IncompleteKey;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.cloud.datastore.StructuredQuery.CompositeFilter;
import com.google.common.collect.Iterators;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javax.servlet.http.HttpServletRequest;

public class UtilityFunctions {

  /*
   * Extracts the value of fieldName attribute from jsonObject if present
   * and returns defaultValue if it is not or the value is empty
   */
  public static String getFieldFromJsonObject(
      JsonObject jsonObject, String fieldName, String defaultValue) {
    if (jsonObject.has(fieldName)) {
      String fieldValue = jsonObject.get(fieldName).getAsString();
      return (fieldValue.length() == 0) ? defaultValue : fieldValue;
    }
    return defaultValue;
  }

  // Adds a comment with the given metadata to the database
  public static void addToDatastore(
      String name,
      String email,
      long dateTime,
      String comment,
      long parentId,
      long rootId,
      boolean isReply,
      long upvotes,
      long downvotes) {
    if ((isReply && (parentId == 0)) || (isReply && (rootId == 0))) {
      return;
    }
    Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    KeyFactory keyFactory = datastore.newKeyFactory().setKind("Comment");
    IncompleteKey key = keyFactory.setKind("Comment").newKey();
    FullEntity<IncompleteKey> thisComment =
        FullEntity.newBuilder(key)
            .set("name", name)
            .set("email", email)
            .set("time", dateTime)
            .set("comment", comment)
            .set("parentid", parentId)
            .set("rootid", rootId)
            .set("upvotes", upvotes)
            .set("score", upvotes - downvotes)
            .set("userid", getCurrentUserId())
            .build();
    Entity inserted = datastore.add(thisComment);

    long commentId = inserted.getKey().getId();
    addTimestampToDatastore(commentId, rootId, dateTime);
  }

  /*
   * Adds an entry to datastore to represent that comment 'commentId' with root Id 'rootId'
   * was submitted at time 'time'
   */
  public static void addTimestampToDatastore(long commentId, long rootId, long time) {
    Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    KeyFactory dateKeyFactory = datastore.newKeyFactory().setKind("DateEntry");
    IncompleteKey dateKey = dateKeyFactory.setKind("DateEntry").newKey();
    FullEntity<IncompleteKey> thisCommentDate =
        FullEntity.newBuilder(dateKey)
            .set("commentid", commentId)
            .set("rootid", rootId)
            .set("time", time)
            .build();
    datastore.add(thisCommentDate);
  }

  // updates the stored timestamp of comment 'commentId' to be newTime
  public static void editTimestampInDatastore(long commentId, long newTime) {
    Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    Query<Entity> query =
        Query.newEntityQueryBuilder()
            .setKind("DateEntry")
            .setFilter(PropertyFilter.eq("commentid", commentId))
            .build();
    QueryResults<Entity> results = datastore.run(query);

    // This comment's timestamp has never been registered (impossible)
    if (!results.hasNext()) {
      return;
    } else {
      Entity timestamp = results.next();
      // There is more than one timestamp for this comment (impossible)
      if (results.hasNext()) {
        return;
      }
      Entity updatedTimestamp = Entity.newBuilder(timestamp).set("time", newTime).build();
      datastore.update(updatedTimestamp);
    }
  }

  /*
   * Adds a vote made by user userId on comment commentId which is an upvote if isUpvote
   * is true and a downvote otherwise to the database to prevent them from voting multiple
   * times on the same comment
   */
  public static void addVoteToDatastore(String userId, long commentId, boolean isUpvote) {
    Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    KeyFactory keyFactory = datastore.newKeyFactory().setKind("Vote");
    IncompleteKey key = keyFactory.setKind("Vote").newKey();
    FullEntity<IncompleteKey> thisVote =
        FullEntity.newBuilder(key)
            .set("userid", userId)
            .set("commentid", commentId)
            .set("isupvote", isUpvote)
            .build();
    datastore.add(thisVote);
  }

  /*
   * Returns 1 if the user userId has upvoted comment commentId, -1 if
   * they have downvoted it and 0 if they have not voted for it
   */
  public static int getVoteInDatastore(String userId, long commentId) {
    Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    Query<Entity> query =
        Query.newEntityQueryBuilder()
            .setKind("Vote")
            .setFilter(
                CompositeFilter.and(
                    PropertyFilter.eq("userid", userId), PropertyFilter.eq("commentid", commentId)))
            .build();
    QueryResults<Entity> results = datastore.run(query);
    // The current user has not voted for this comment
    if (!results.hasNext()) {
      return 0;
    } else {
      Entity vote = results.next();
      // If there is more than one entry for a user-vote pair (impossible)
      if (results.hasNext()) {
        return 0;
      }
      int voteValue = (vote.getBoolean("isupvote")) ? 1 : -1;
      return voteValue;
    }
  }

  // Removes user userId's vote on comment commentId from database
  public static void removeVoteInDatastore(String userId, long commentId) {
    Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    Query<Key> query =
        Query.newKeyQueryBuilder()
            .setKind("Vote")
            .setFilter(
                CompositeFilter.and(
                    PropertyFilter.eq("userid", userId), PropertyFilter.eq("commentid", commentId)))
            .build();
    Key[] keys = Iterators.toArray(datastore.run(query), Key.class);
    datastore.delete(keys);
  }

  /*
   * Get value for fieldName for request if present. Return defaultValue if fieldName is not present
   * or the associated value is empty. Raise an exception if fieldName is mapped to multiple values.
   */
  public static String getFieldFromResponse(
      HttpServletRequest request, String fieldName, String defaultValue) {
    String[] defaultArr = {defaultValue};
    String[] fieldValues = request.getParameterMap().getOrDefault(fieldName, defaultArr);
    if (fieldValues.length > 1) {
      throw new IllegalArgumentException("Found multiple values for single key in form");
    } else {
      String userValue = fieldValues[0];
      if (userValue.length() == 0) {
        return defaultValue;
      } else {
        return userValue;
      }
    }
  }

  // Parses the string as a Json Object and returns it
  public static JsonObject stringToJsonObject(String field) {
    JsonParser parser = new JsonParser();
    JsonObject obj = parser.parse(field).getAsJsonObject();
    return obj;
  }

  // Gets the unique ID of the user currently logged in
  public static String getCurrentUserId() {
    UserService userService = UserServiceFactory.getUserService();
    String userId = userService.getCurrentUser().getUserId();
    return userId;
  }

  /*
   * If language 'langCode' has never been requested, register that it has now been requested once.
   * Otherwise, increase its request count by 1 in the database.
   */
  public static void updateLangInDatastoreIfPresent(String langCode) {
    Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    Query<Entity> query =
        Query.newEntityQueryBuilder()
            .setKind("CommentLang")
            .setFilter(PropertyFilter.eq("lang", langCode))
            .build();
    QueryResults<Entity> results = datastore.run(query);

    if (results.hasNext()) {
      Entity lang = results.next();
      // There are multiple entries for this language (impossible)
      if (results.hasNext()) {
        return;
      }
      long numCommentsInLang = lang.getLong("comments");
      Entity updatedLang = Entity.newBuilder(lang).set("comments", numCommentsInLang + 1).build();
      datastore.update(updatedLang);
    } else {
      // This language has never been requested so add it to datastore with one request
      KeyFactory keyFactory = datastore.newKeyFactory().setKind("CommentLang");
      IncompleteKey key = keyFactory.setKind("CommentLang").newKey();
      FullEntity<IncompleteKey> thisLang =
          FullEntity.newBuilder(key).set("lang", langCode).set("comments", 1).build();
      datastore.add(thisLang);
    }
  }
}
