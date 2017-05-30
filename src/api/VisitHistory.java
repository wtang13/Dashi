package api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import db.DBConnection;
import db.MongoDBConnection;
import db.MySQLDBConnection;

/**
 * Servlet implementation class VisitHistory
 */
@WebServlet("/history")
public class VisitHistory extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final DBConnection connection = new MongoDBConnection();

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public VisitHistory() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			JSONArray array = null;
			if (request.getParameterMap().containsKey("user_id")) {
				String userId = request.getParameter("user_id");
				Set<String> visited_business_id = connection.getVisitedRestaurants(userId);
				array = new JSONArray();
				for (String id : visited_business_id) {
					array.put(connection.getRestaurantsById(id, true));
				}
				RpcParser.writeOutput(response, array);
			} else {
				RpcParser.writeOutput(response, new JSONObject().put("status", "InvalidParameter"));
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

	}

	/**
	 * VisitHistory can return a list of restaurant ids that the user has visited?
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			JSONArray allVisitedRestaurant = new JSONArray();
			JSONObject input = RpcParser.parseInput(request);
			String userId = (String) input.get("user_id");
			// check valid user
			String name = connection.getFirstLastName(userId);
			if (name == null || name.equals("")) {
				JSONObject errorUser = new JSONObject();
				errorUser.put("status", "InvalidUserId");
				allVisitedRestaurant.put(errorUser);
				RpcParser.writeOutput(response, allVisitedRestaurant);
			} else {
				//check valid restaurant, if already visited, don't do set in following step
				JSONArray array = (JSONArray) input.get("visited");
				List<String> visitedRestaurants = new ArrayList<>();
				for (int i = 0; i < array.length(); i++) {
					String businessId = (String) array.get(i);

					visitedRestaurants.add(businessId);
				}
				/*
				 * What is the definition of "invalid restaurant"?
				 * A restaurant will be invalid if it is not in our DB or it is not in yelp?
				 * I implement a method always return true here, need more changes
				 * If it is not in DB: just do search in DB where bussiness_id = i
				 * If it is not in yelp, we need to search in yelp with bussiness_id, 
				 * which is a get method and in a new servlet, and will this operation violate
				 * our design principle : one api do one thing
				 **/
				boolean invalid = false;
				for (String i : visitedRestaurants) {
					boolean result = connection.validRestaurantById(i);
					if (result == false) {
						invalid = true;
						break;
					}
				}
				if (invalid) {
					JSONObject errorRestaurant = new JSONObject();
					errorRestaurant.put("status", "InvalidRestaurant");
					allVisitedRestaurant.put(errorRestaurant);
					RpcParser.writeOutput(response, allVisitedRestaurant);
				} else {
					// should be valid user(in our DB ) valid restaurant here
					// can't set already seted restaurant
					// approach 1 : change insert statement in set: if not in, then insert
					//approach 2: delete duplicate element first then insert
					Iterator<String> ite = visitedRestaurants.iterator();
					while (ite.hasNext()) {
						String temp = ite.next();
						if (connection.validVisitedRestaurantById(temp)) {
							ite.remove();// auto remove temp
						}
					}
					if (visitedRestaurants.size() !=  0) { // have something to set
						connection.setVisitedRestaurants(userId, visitedRestaurants);
					} 
					Set<String> restaurantInfoInDB = connection.getVisitedRestaurants(userId);
					int j = 1;
					for (String i : restaurantInfoInDB) {
						JSONObject temp = new JSONObject();
						temp.put("NO."+j+" restaurant", i);
						j++;
						allVisitedRestaurant.put(temp);
					}
					RpcParser.writeOutput(response, allVisitedRestaurant);

				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

	}


	/*
	 * Unset the visited history
	 * */
	protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			JSONArray allVisitedRestaurant = new JSONArray();
			JSONObject input = RpcParser.parseInput(request);
			String userId = (String) input.get("user_id");
			// check valid user
			String name = connection.getFirstLastName(userId);
			if (name == null || name.equals("")) {
				JSONObject errorUser = new JSONObject();
				errorUser.put("status", "InvalidUserId");
				allVisitedRestaurant.put(errorUser);
				RpcParser.writeOutput(response, allVisitedRestaurant);
			} else {
				JSONArray array = (JSONArray) input.get("visited");
				List<String> visitedRestaurants = new ArrayList<>();
				for (int i = 0; i < array.length(); i++) {
					String businessId = (String) array.get(i);

					visitedRestaurants.add(businessId);
				}
				// should be valid user(in our DB ) 
				connection.unsetVisitedRestaurants(userId, visitedRestaurants);
				Set<String> restaurantInfoInDB = connection.getVisitedRestaurants(userId);
				int j = 1;
				for (String i : restaurantInfoInDB) {
					JSONObject temp = new JSONObject();
					temp.put("NO."+j+" restaurant", i);
					j++;
					allVisitedRestaurant.put(temp);
				}
				RpcParser.writeOutput(response, allVisitedRestaurant);

			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

	}
}
