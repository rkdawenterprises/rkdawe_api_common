package net.ddns.rkdawenterprises.rkdawe_api_common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Get_weather_station_data_GET_response
{
    public Weather_data weather_data;
    public String success;

    public static final Gson m_GSON = new GsonBuilder().disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    public static String serialize_to_JSON( Get_weather_station_data_GET_response object )
    {
        return m_GSON.toJson( object );
    }

    public static Get_weather_station_data_GET_response deserialize_from_JSON( String string_JSON )
    {
        Get_weather_station_data_GET_response object = null;
        try
        {
            object = m_GSON.fromJson( string_JSON,
                    Get_weather_station_data_GET_response.class );
        }
        catch( com.google.gson.JsonSyntaxException exception )
        {
            System.out.println( "Bad data format for Get_weather_station_data_GET_response: " + exception );
            System.out.println( ">>>" + string_JSON + "<<<" );
        }

        return object;
    }

    public String serialize_to_JSON()
    {
        return serialize_to_JSON( this );
    }
}
