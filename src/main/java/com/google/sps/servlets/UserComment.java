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

import com.google.auto.value.AutoValue;

@AutoValue
abstract class UserComment {

  public enum voteStatus {
    UPVOTED,
    DOWNVOTED,
    NOTVOTED
  }

  static UserComment create(
      String name,
      String email,
      String comment,
      long timestamp,
      long id,
      long parentId,
      long rootId,
      long upvotes,
      long downvotes,
      boolean isEditable,
      voteStatus votingStatus) {
    return new AutoValue_UserComment(
        name,
        email,
        comment,
        timestamp,
        id,
        parentId,
        rootId,
        upvotes,
        downvotes,
        isEditable,
        votingStatus);
  }

  /*
   * Represents the name of the commenter
   * Default Value: Anonymous
   * Invariants: -
   * The default value is also used for authors of replies, whose names
   * are not currenty tracked
   */
  abstract String name();

  /*
   * Represents the email ID of the commenter
   * Default Value: janedoe@gmail.com
   * Invariants: -
   * The default value is also used for authors of replies, whose email IDs
   * are not currenty tracked
   */
  abstract String email();

  /*
   * Represents the content of the comment
   * Default Value: -
   * Invariants: - Must be non-empty
   */
  abstract String comment();

  /*
   * Represents the difference, measured in milliseconds, between the
   * time of form submission by the client and midnight, January 1, 1970 UTC
   * Default Value: The difference, measured in milliseconds, between the
   * time of form receipt by the server and midnight, January 1, 1970 UTC
   * Invariants: - Is always non-negative
   */
  abstract long timestamp();

  /*
   * Represents the ID of the Datastore entity for this comment
   * Default Value: -
   * Invariants: -
   */
  abstract long id();

  /*
   * Represents the ID of the parent comment of this comment, if
   * this is a reply and 0 if this is a root comment.
   * Default Value: -
   * Invariants: Always non-negative
   */
  abstract long parentId();

  /*
   * Represents the ID of the root of the comment tree that
   * this reply is part of. If this is the root, rootId is
   * 0.
   * Default Value: -
   * Invariants: Always non-negative
   */
  abstract long rootId();

  /*
   * Represents the number of upvotes this comment has received
   * Invariants: Always non-negative
   */
  abstract long upvotes();

  /*
   * Represents the number of downvotes this comment has received
   * Invariants: Always non-negative
   */
  abstract long downvotes();

  /*
   * Represents whether the current user is the author of this
   * comment and can edit it
   */
  abstract boolean isEditable();

  /*
   * Represents whether the current user has upvoted (UPVOTED), downvoted
   * (DOWNVOTED) or not voted for (NOTVOTED) the given comment.
   * Invariants: The above conditions are mutually exclusive.
   */
  abstract voteStatus votingStatus();
}
