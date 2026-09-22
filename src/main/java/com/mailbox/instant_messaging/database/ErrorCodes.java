package com.mailbox.instant_messaging.database;

public enum ErrorCodes {
  NO_ACTION_SPECIFIED(101),
  BAD_ACTION(102),
  DB_ERROR(500);

  private final int code;

  ErrorCodes(int code) {
    this.code = code;
  }

  public int getCode() {
    return this.code;
  }
}
