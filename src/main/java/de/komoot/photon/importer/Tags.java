/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.komoot.photon.importer;

/**
 * Holds the String representation of the json properties that are used in the reply.
 * This class is intended to hold certain strings in order to avoid hardcoding (repeatuing
 * and thus potential errors) of strings.
 * 
 * @date: 14.06.14
 * @author jseibl
 */
public class Tags {
	public static final String KEY_TYPE = "type";
	public static final String KEY_GEOMETRY = "geometry";
	public static final String KEY_PROPERTIES = "properties";
	public static final String KEY_COORDINATES = "coordinates";
	
	public static final String KEY_LAT = "lat";
	public static final String KEY_LON = "lon";
	
	public static final String VALUE_FEATURE = "Feature";
	public static final String VALUE_POINT = "Point";
}
