package org.keycloak.custom.storage.user;

public class UserStoreQueries {
	// User retrieval queries
	public final static String GET_USER_BY_ID = "select id,userName,phone,email from UserStore where id = ?1";
	public final static String GET_USER_BY_NAME = "select id,userName,phone,email from UserStore where userName = ?1";
	public final static String GET_ALL_USERS = "select id,userName,phone,email from UserStore";
	public final static String GET_USER_COUNT = "select count(id) from UserStore";

	// User authentication queries
	public final static String AUTH_USER_PASS = "select b.userName, b.passPhrase from UserStore a, UserPass b where a.userName=b.userName and b.passPhrase=?1 and a.userName= ?2";

	// Stored Procedure to get user by user name
	public final static String GET_USER_BY_NAME_SP = "GetUserByName";
	public final static String GET_USER_BY_NAME_SP_ARG1 = "user_name";
}
