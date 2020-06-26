package com.google.sps.servlets;

import javax.annotation.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_DayComments extends DayComments {

  private final int rootComments;

  private final int replies;

  AutoValue_DayComments(
      int rootComments,
      int replies) {
    this.rootComments = rootComments;
    this.replies = replies;
  }

  @Override
  int rootComments() {
    return rootComments;
  }

  @Override
  int replies() {
    return replies;
  }

  @Override
  public String toString() {
    return "DayComments{"
        + "rootComments=" + rootComments + ", "
        + "replies=" + replies
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof DayComments) {
      DayComments that = (DayComments) o;
      return this.rootComments == that.rootComments()
          && this.replies == that.replies();
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= rootComments;
    h$ *= 1000003;
    h$ ^= replies;
    return h$;
  }

}
