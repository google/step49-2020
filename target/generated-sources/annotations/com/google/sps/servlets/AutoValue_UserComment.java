package com.google.sps.servlets;

import javax.annotation.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_UserComment extends UserComment {

  private final String name;

  private final String email;

  private final String comment;

  private final long timestamp;

  private final long id;

  private final long parentId;

  private final long rootId;

  private final long upvotes;

  private final long downvotes;

  private final boolean isEditable;

  private final UserComment.voteStatus votingStatus;

  AutoValue_UserComment(
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
      UserComment.voteStatus votingStatus) {
    if (name == null) {
      throw new NullPointerException("Null name");
    }
    this.name = name;
    if (email == null) {
      throw new NullPointerException("Null email");
    }
    this.email = email;
    if (comment == null) {
      throw new NullPointerException("Null comment");
    }
    this.comment = comment;
    this.timestamp = timestamp;
    this.id = id;
    this.parentId = parentId;
    this.rootId = rootId;
    this.upvotes = upvotes;
    this.downvotes = downvotes;
    this.isEditable = isEditable;
    if (votingStatus == null) {
      throw new NullPointerException("Null votingStatus");
    }
    this.votingStatus = votingStatus;
  }

  @Override
  String name() {
    return name;
  }

  @Override
  String email() {
    return email;
  }

  @Override
  String comment() {
    return comment;
  }

  @Override
  long timestamp() {
    return timestamp;
  }

  @Override
  long id() {
    return id;
  }

  @Override
  long parentId() {
    return parentId;
  }

  @Override
  long rootId() {
    return rootId;
  }

  @Override
  long upvotes() {
    return upvotes;
  }

  @Override
  long downvotes() {
    return downvotes;
  }

  @Override
  boolean isEditable() {
    return isEditable;
  }

  @Override
  UserComment.voteStatus votingStatus() {
    return votingStatus;
  }

  @Override
  public String toString() {
    return "UserComment{"
        + "name=" + name + ", "
        + "email=" + email + ", "
        + "comment=" + comment + ", "
        + "timestamp=" + timestamp + ", "
        + "id=" + id + ", "
        + "parentId=" + parentId + ", "
        + "rootId=" + rootId + ", "
        + "upvotes=" + upvotes + ", "
        + "downvotes=" + downvotes + ", "
        + "isEditable=" + isEditable + ", "
        + "votingStatus=" + votingStatus
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof UserComment) {
      UserComment that = (UserComment) o;
      return this.name.equals(that.name())
          && this.email.equals(that.email())
          && this.comment.equals(that.comment())
          && this.timestamp == that.timestamp()
          && this.id == that.id()
          && this.parentId == that.parentId()
          && this.rootId == that.rootId()
          && this.upvotes == that.upvotes()
          && this.downvotes == that.downvotes()
          && this.isEditable == that.isEditable()
          && this.votingStatus.equals(that.votingStatus());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= name.hashCode();
    h$ *= 1000003;
    h$ ^= email.hashCode();
    h$ *= 1000003;
    h$ ^= comment.hashCode();
    h$ *= 1000003;
    h$ ^= (int) ((timestamp >>> 32) ^ timestamp);
    h$ *= 1000003;
    h$ ^= (int) ((id >>> 32) ^ id);
    h$ *= 1000003;
    h$ ^= (int) ((parentId >>> 32) ^ parentId);
    h$ *= 1000003;
    h$ ^= (int) ((rootId >>> 32) ^ rootId);
    h$ *= 1000003;
    h$ ^= (int) ((upvotes >>> 32) ^ upvotes);
    h$ *= 1000003;
    h$ ^= (int) ((downvotes >>> 32) ^ downvotes);
    h$ *= 1000003;
    h$ ^= isEditable ? 1231 : 1237;
    h$ *= 1000003;
    h$ ^= votingStatus.hashCode();
    return h$;
  }

}
