/**
 * This file contains the Destination class.
 */
package org.datasift;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.datasift.pushsubscription.Http;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * The Destination class represents a data Destination.
 * 
 * @author MediaSift
 * @version 0.1
 */
abstract public class PushSubscription {
	/**
	 * Hash type constants.
	 */
	public final static String HASH_TYPE_STREAM   = "stream";
	public final static String HASH_TYPE_HISTORIC = "historic";
	
	/**
	 * Status constants.
	 */
	public final static String STATUS_ACTIVE    = "active";
	public final static String STATUS_PAUSED    = "paused";
	public final static String STATUS_STOPPED   = "stopped";
	public final static String STATUS_FINISHING = "finishing";
	public final static String STATUS_FINISHED  = "finished";
	public final static String STATUS_DELETED   = "deleted";
	
	/**
	 * Order by constants.
	 */
	public final static String ORDER_ID = "id";
	public final static String ORDER_CREATED_AT = "created_at";
	
	/**
	 * Order direction constants.
	 */
	public final static String ORDERDIR_ASC = "asc";
	public final static String ORDERDIR_DESC = "desc";
	
	/**
	 * Get a push subscription by ID.
	 * 
	 * @param User user The user who owns the subscription.
	 * @param int  id   The subscription ID.
	 * @return PushSubscription
	 * @throws EAPIError
	 * @throws EAccessDenied
	 * @throws EInvalidData
	 */
	static public PushSubscription get(User user, int id) throws EAPIError, EAccessDenied, EInvalidData {
		HashMap<String, String> params = new HashMap<String, String>();

		params.put("id", String.valueOf(id));
		
		JSONObject res = user.callAPI("push/get", params);
		String output_type = null;
		try {
			output_type = res.getString("output_type");
		} catch (JSONException e) {
			throw new EAPIError("No output_type in the response");
		}
		return factory(user, output_type, res);
	}

	/**
	 * Get a list of push subscriptions in the given user's account. Limited
	 * to 100 results.Results will be returned in ascending order by creation
	 * date.
	 * 
	 * @param User user
	 * @return ArrayList<PushSubscription>
	 * @throws EInvalidData
	 * @throws EAPIError
	 * @throws EAccessDenied
	 */
	static public ArrayList<PushSubscription> list(User user) throws EInvalidData, EAPIError, EAccessDenied {
		return list(user, 1, 100);
	}
	
	/**
	 * Get a page of push subscriptions in the given user's account, where
	 * each page contains up to 20 items. Results will be returned in
	 * ascending order by creation date.
	 * 
	 * @param User user The user.
	 * @param int  page The page number to fetch.
	 * @return ArrayList<PushSubscription>
	 * @throws EInvalidData
	 * @throws EAPIError
	 * @throws EAccessDenied
	 */
	static public ArrayList<PushSubscription> list(User user, int page) throws EInvalidData, EAPIError, EAccessDenied {
		return list(user, page, 20);
	}
	
	/**
	 * Get a page of push subscriptions in the given user's account, where
	 * each page contains up to per_page items. Results will be returned in
	 * ascending order by creation date.
	 * 
	 * @param User user     The user.
	 * @param int  page     The page number to fetch.
	 * @param int  per_page The number of items per page.
	 * @return ArrayList<PushSubscription>
	 * @throws EInvalidData
	 * @throws EAPIError
	 * @throws EAccessDenied
	 */
	static public ArrayList<PushSubscription> list(User user, int page, int per_page) throws EInvalidData, EAPIError, EAccessDenied {
		return list(user, page, per_page, ORDER_CREATED_AT, ORDERDIR_ASC, false);
	}
	
	/**
	 * Get a page of push subscriptions in the given user's account, where
	 * each page contains up to per_page items. Results will be ordered
	 * according to the supplied ordering parameters.
	 * 
	 * @param User user                The user.
	 * @param int  page                The page number to fetch.
	 * @param int  per_page            The number of items per page.
	 * @param String order_by          The field on which to order the results.
	 * @param String order_dir         The direction of the ordering.
	 * @param boolean include_finished True to include subscriptions against
	 *                                 finished historic queries.
	 * @return ArrayList<PushSubscription>
	 * @throws EInvalidData
	 * @throws EAPIError
	 * @throws EAccessDenied
	 */
	static public ArrayList<PushSubscription> list(User user, int page, int per_page, String order_by, String order_dir, boolean include_finished) throws EInvalidData, EAPIError, EAccessDenied {
		HashMap<String, String> params = new HashMap<String, String>();

		if (page < 1) {
			throw new EInvalidData("The specified page number is invalid");
		}
		
		if (per_page < 1) {
			throw new EInvalidData("The specified per_page value is invalid");
		}
		
		if (order_by != ORDER_ID && order_by != ORDER_CREATED_AT) {
			throw new EInvalidData("The specified order_by is not supported");
		}

		if (order_dir != ORDERDIR_ASC && order_dir != ORDERDIR_DESC) {
			throw new EInvalidData("The specified order_dir is not supported");
		}
		
		params.put("page", String.valueOf(page));
		params.put("per_page", String.valueOf(per_page));
		params.put("order_by", order_by);
		params.put("order_dir", order_dir);

		JSONObject res = user.callAPI("push/get", params);

		ArrayList<PushSubscription> retval = new ArrayList<PushSubscription>();

		try {
	        JSONArray subscriptions = res.getJSONArray("subscriptions");
	        for (int i = 0; i < subscriptions.length(); i++) {
	        	JSONObject subscription = subscriptions.getJSONObject(i);
	            retval.add(factory(user, subscription.getString("output_type"), subscription));
	        }
		} catch (JSONException e) {
			throw new EAPIError("Failed to read the subscriptions from the response");
		}
		
		return retval;
	}
	
	/**
	 * Factory method to create output_type-specific, empty PushSubscription
	 * objects.
	 * 
	 * @param User   user        The user requesting the object.
	 * @param String output_type The type of the object.
	 * @return PushSubscription
	 * @throws EInvalidData
	 */
	static public PushSubscription factory(User user, String output_type) throws EInvalidData {
		if (output_type.toLowerCase().equals("http")) {
			return new Http(user);
		}
		
		throw new EInvalidData("Unknown output type \"" + output_type + "\"");
	}
	
	/**
	 * Factory method to create output_type-specific PushSubscription objects.
	 * 
	 * @param User       user        The user requesting the object.
	 * @param String     output_type The type of the object,
	 * @param JSONObject json        The data with which to initialise the
	 *                               object.
	 * @return PushSubscription
	 * @throws EInvalidData
	 */
	static public PushSubscription factory(User user, String output_type, JSONObject json) throws EInvalidData {
		if (output_type.toLowerCase().equals("http")) {
			return new Http(user, json);
		}
		
		throw new EInvalidData("Unknown output type \"" + output_type + "\"");
	}

	public static PushSubscription factory(User user, String output_type,
			String hash_type, String hash, String name) throws EInvalidData {
		return factory(user, output_type, hash_type, hash, name, "");
	}
	
	public static PushSubscription factory(User user, String output_type,
			String hash_type, String hash, String name, String initial_status) throws EInvalidData {
		PushSubscription retval = factory(user, output_type);
		if (!hash_type.equals(HASH_TYPE_STREAM) && !hash_type.equals(HASH_TYPE_HISTORIC)) {
			throw new EInvalidData("Unknown hash type: \"" + hash_type + "\"");
		}
		retval._output_type = output_type;
		retval._hash_type = hash_type;
		retval._hash = hash;
		retval._name = name;
		retval._status = initial_status;
		return retval;
	}

	protected User _user = null;
	protected int _id = 0;
	protected String _name = "";
	protected Date _created_at = null;
	protected String _status = "";
	protected String _hash = "";
	protected String _hash_type = "";
	protected String _output_type = "";
	protected JSONObject _output_params = new JSONObject();
	protected boolean _deleted = false;
	
	public PushSubscription(User user) {
		_user = user;
	}
	
	public PushSubscription(User user, JSONObject json) throws EInvalidData {
		_user = user;
		init(json);
	}
	
	private void init(JSONObject json) throws EInvalidData {
		try {
			_id = json.getInt("id");
		} catch (JSONException e) {
			throw new EInvalidData("No id found");
		}
		
		try {
			_name = json.getString("name");
		} catch (JSONException e) {
			throw new EInvalidData("No name found");
		}
		
		try {
			_created_at = new Date(json.getLong("created_at") * 1000);
		} catch (JSONException e) {
			throw new EInvalidData("No created_at found");
		}
		
		try {
			_status = json.getString("status");
		} catch (JSONException e) {
			throw new EInvalidData("No status found");
		}
		
		try {
			_hash_type = json.getString("hash_type");
		} catch (JSONException e) {
			throw new EInvalidData("No hash_type found");
		}
		
		try {
			_hash = json.getString("hash");
		} catch (JSONException e) {
			throw new EInvalidData("No hash found");
		}
		
		try {
			_output_type = json.getString("output_type");
		} catch (JSONException e) {
			throw new EInvalidData("No output_type found");
		}
		
		try {
			_output_params = json.getJSONObject("output_params");
		} catch (JSONException e) {
			throw new EInvalidData("No output_params found");
		}
	}
	
	public int getId() {
		return _id;
	}
	
	public String getName() {
		return _name;
	}
	
	public void setName(String name) throws EInvalidData {
		if (isDeleted()) {
			throw new EInvalidData("Cannot modify a deleted subscription");
		}
		_name = name;
	}
	
	public Date getCreatedAt() {
		return _created_at;
	}

	public String getStatus() {
		return _status;
	}
	
	public boolean isDeleted() {
		return getStatus() == STATUS_DELETED;
	}
	
	public String getHashType() {
		return _hash_type;
	}
	
	public String getHash() {
		return _hash;
	}
	
	public String getOutputType() {
		return _output_type;
	}
	
	public void save() throws EInvalidData, EAPIError, EAccessDenied {
		HashMap<String, String> params = new HashMap<String, String>();

		String endpoint = "push/";
		if (getId() == 0) {
			// Never saved, create it
			endpoint += "create";
			
			// Add the hash/playback_id
			if (getHashType().equals(HASH_TYPE_STREAM)) {
				params.put("hash", getHash());
			} else if (getHashType().equals(HASH_TYPE_HISTORIC)) {
				params.put("playback_id", getHash());
			} else {
				throw new EInvalidData("Unknown hash_type: \"" + getHashType() + "\"");
			}
			
			// Output type
			params.put("output_type", getOutputType());
			
			// Add the initial status if it's not empty
			if (getStatus().length() > 0) {
				params.put("initial_status", getStatus());
			}
		} else {
			// Already been saved, do an update
			endpoint += "update";
			
			// ID
			params.put("id", String.valueOf(getId()));
		}

		// Name and output_params are sent whether creating or updating
		params.put("name", getName());
		params.put("output_params", _output_params.toString());
		
		// Call the API and pass the returned object into init to update this object
		init(_user.callAPI(endpoint, params));
	}
	
	public void delete() throws EAPIError, EAccessDenied {
		if (getId() > 0) {
			HashMap<String, String> params = new HashMap<String, String>();
			params.put("id", String.valueOf(getId()));
			_user.callAPI("push/delete", params);
		}
		_status = STATUS_DELETED;
	}
}
