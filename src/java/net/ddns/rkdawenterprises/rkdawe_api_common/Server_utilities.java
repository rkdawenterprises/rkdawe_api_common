
package net.ddns.rkdawenterprises.rkdawe_api_common;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import javax.servlet.ServletContext;

import org.apache.commons.io.FileUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.ddns.rkdawenterprises.rkdawe_webapp.User;

public class Server_utilities
{
    public static class Database_info
    {
        public final String AUTHENTICATION_DATABASE_HOST;
        public final String AUTHENTICATION_DATABASE_NAME;
        public final String AUTHENTICATION_DATABASE_USER;
        public final String AUTHENTICATION_DATABASE_PASS;

        public Database_info( String authentication_database_host,
                              String authentication_database_name,
                              String authentication_database_user,
                              String authentication_database_pass )
        {
            this.AUTHENTICATION_DATABASE_HOST = authentication_database_host;
            this.AUTHENTICATION_DATABASE_NAME = authentication_database_name;
            this.AUTHENTICATION_DATABASE_USER = authentication_database_user;
            this.AUTHENTICATION_DATABASE_PASS = authentication_database_pass;
        }

        public static String serialize_to_JSON( Database_info object )
        {
            Gson gson = new GsonBuilder().disableHtmlEscaping()
                                         .setPrettyPrinting()
                                         .create();
            return gson.toJson( object );
        }

        public static Database_info deserialize_from_JSON( String string_JSON )
        {
            Database_info object = null;
            try
            {
                Gson gson = new GsonBuilder().disableHtmlEscaping()
                                             .setPrettyPrinting()
                                             .create();
                object = gson.fromJson( string_JSON,
                                        Database_info.class );
            }
            catch( com.google.gson.JsonSyntaxException exception )
            {
                System.out.println( "Bad data format for Database_info: " + exception );
                System.out.println( ">>>" + string_JSON + "<<<" );
            }

            return object;
        }

        public String serialize_to_JSON()
        {
            return serialize_to_JSON( this );
        }
    }

    public static Database_info get_database_info()
        throws IllegalArgumentException, IOException
    {
        String database_info_JSON_as_string = Files.readString(Path.of("/opt/home/tomcat/database_info.json"));
        return Database_info.deserialize_from_JSON( database_info_JSON_as_string );
    }

    /**
     * Authenticates user:password by matching it to a record in the authentication
     * database. If the username is found, and the hashed password matches the
     * hashed password in the database, the given User object is updated with the
     * information in the authentication database.
     * 
     * If the username is found but the hashed password does not match, the given
     * User object is also updated with the information in the authentication
     * database. But in this case an exception is thrown.
     *
     * The DATETIME values in the database are assumed to be UTC, so all times
     * returned are also UTC.
     * 
     * @param username
     * @param password
     * @param user            The User object to update with the database
     *                        information.
     *
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws InvalidKeySpecException
     * @throws NoSuchAlgorithmException
     * @throws IOException 
     */
    public static void authenticate( String username,
                                     String password,
                                     User user )
            throws SQLException, ClassNotFoundException, NoSuchAlgorithmException, InvalidKeySpecException,
            DateTimeException, IllegalArgumentException, IOException
    {
        Class.forName( "com.mysql.cj.jdbc.Driver" );

        Database_info database_info = get_database_info();
        String database_URI = "jdbc:mysql://" + database_info.AUTHENTICATION_DATABASE_HOST + ":3306/"
                + database_info.AUTHENTICATION_DATABASE_NAME + "?serverTimezone=UTC";

        try( Connection connection = DriverManager.getConnection( database_URI,
                                                                  database_info.AUTHENTICATION_DATABASE_USER,
                                                                  database_info.AUTHENTICATION_DATABASE_PASS ) )
        {
            String query = "SELECT * FROM accounts WHERE username = ?";
            try( PreparedStatement prepared_statement = connection.prepareStatement( query ) )
            {
                prepared_statement.setString( 1,
                                              username );

                ResultSet result_set = prepared_statement.executeQuery();
                if( result_set.next() )
                {
                    user.id = result_set.getInt( "id" );
                    user.username = result_set.getString( "username" );
                    user.email = result_set.getString( "email" );

                    LocalDateTime created_at_as_local_date_time = result_set.getObject( "created_at",
                                                                                        LocalDateTime.class );
                    ZonedDateTime created_at_as_zoned_date_time = ZonedDateTime.of( created_at_as_local_date_time,
                                                                                    ZoneId.of( "UTC" ) );
                    user.created_at = Instant.from( created_at_as_zoned_date_time );

                    LocalDateTime last_log_in_as_local_date_time = result_set.getObject( "last_log_in",
                                                                                         LocalDateTime.class );
                    if( last_log_in_as_local_date_time != null )
                    {
                        ZonedDateTime last_log_in_as_zoned_date_time = ZonedDateTime.of( last_log_in_as_local_date_time,
                                                                                         ZoneId.of( "UTC" ) );
                        user.last_log_in = Instant.from( last_log_in_as_zoned_date_time );
                    }

                    LocalDateTime last_invalid_attempt_as_local_date_time = result_set.getObject( "last_invalid_attempt",
                                                                                                  LocalDateTime.class );
                    if( last_invalid_attempt_as_local_date_time != null )
                    {
                        ZonedDateTime last_invalid_attempt_as_zoned_date_time = ZonedDateTime.of( last_invalid_attempt_as_local_date_time,
                                                                                                  ZoneId.of( "UTC" ) );
                        user.last_invalid_attempt = Instant.from( last_invalid_attempt_as_zoned_date_time );
                    }

                    user.invalid_attempts = result_set.getInt( "invalid_attempts" );
                    if( ( user.invalid_attempts > 5 ) && ( last_invalid_attempt_as_local_date_time != null ) )
                    {
                        ZonedDateTime last_invalid_attempt_as_zoned_date_time = ZonedDateTime.of( last_invalid_attempt_as_local_date_time,
                                                                                                  ZoneId.of( "UTC" ) );

                        ZonedDateTime system_now = ZonedDateTime.now( ZoneId.of( "UTC" ) );

                        // System.out.println( "last: " + last_invalid_attempt_as_zoned_date_time );
                        // System.out.println( "now: " + system_now );

                        Duration difference = Duration.between( last_invalid_attempt_as_zoned_date_time,
                                                                system_now );

                        // System.out.println( "difference: " + Utilities.duration_to_string( difference
                        // ) );

                        if( difference.compareTo( Duration.ofMinutes( 10 ) ) < 0 )
                        {
                            throw new IllegalArgumentException( "Too many attempts, temporarily blocked" );
                        }
                    }

                    String password_in_database = result_set.getString( "password" );
                    byte[] salt_bytes = result_set.getBytes( "salt" );
                    String password_hashed = Utilities.hash_password( password,
                                                            salt_bytes );
                    if( password_in_database.equals( password_hashed ) )
                    {
                        user.authenticated = User.AUTHENTICATED.TRUE;
                        return;
                    }
                    else
                    {
                        user.authenticated = User.AUTHENTICATED.FALSE;
                    }
                }
            }
        }

        throw new IllegalArgumentException( "Could not validate username/password" );
    }

    /**
     * Updates the last log in time with now at UTC. Only meant to be called
     * immediately after a successful authentication with the database ID of the
     * user. The last invalid attempt and invalid attempts will be cleared as a
     * result.
     *
     * @param an_ID           The database identifier of the user record to update
     *
     * @return The now at UTC time.
     *
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws IOException 
     * @throws IllegalArgumentException 
     */
    public static Instant update_last_log_in( int an_ID )
            throws SQLException, ClassNotFoundException, IllegalArgumentException, IOException
    {
        Class.forName( "com.mysql.cj.jdbc.Driver" );

        Database_info database_info = get_database_info();
        String database_URI = "jdbc:mysql://" + database_info.AUTHENTICATION_DATABASE_HOST + ":3306/"
                + database_info.AUTHENTICATION_DATABASE_NAME + "?serverTimezone=UTC";

        try( Connection connection = DriverManager.getConnection( database_URI,
                                                                  database_info.AUTHENTICATION_DATABASE_USER,
                                                                  database_info.AUTHENTICATION_DATABASE_PASS ) )
        {
            Instant instant;
            instant = Instant.now()
                             .truncatedTo( ChronoUnit.SECONDS );
            String last_log_in = instant.toString()
                                        .replace( 'T',
                                                  ' ' )
                                        .replace( "Z",
                                                  "" );

            String query = "UPDATE `accounts` SET `last_log_in` = ?, `last_invalid_attempt` = ?, `invalid_attempts` = ? WHERE `accounts`.`id` = ?";
            try( PreparedStatement prepared_statement = connection.prepareStatement( query ) )
            {
                prepared_statement.setString( 1,
                                              last_log_in );
                prepared_statement.setNull( 2,
                                            Types.NULL );
                prepared_statement.setInt( 3,
                                           0 );
                prepared_statement.setInt( 4,
                                           an_ID );
                prepared_statement.executeUpdate();
            }

            return instant;
        }
    }

    /**
     * Updates the last invalid attempted log in time with now at UTC. Only meant to
     * be called immediately after a failed authentication with the database ID of
     * the user. The given invalid attempts will be updated in the database.
     * 
     * Updates the last log in time with now at UTC. Only meant to be called
     * immediately after authenticate with the database ID of the user.
     * 
     * @param an_ID            The database identifier of the user record to update
     * @param invalid_attempts The number of invalid attempts. This value will be
     *                         written to the database.
     * 
     * @return The now at UTC time.
     * 
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws IOException 
     * @throws IllegalArgumentException 
     */
    public static Instant update_last_invalid_attempt( int an_ID,
                                                       int invalid_attempts )
            throws SQLException, ClassNotFoundException, IllegalArgumentException, IOException
    {
        Class.forName( "com.mysql.cj.jdbc.Driver" );

        Database_info database_info = get_database_info();
        String database_URI = "jdbc:mysql://" + database_info.AUTHENTICATION_DATABASE_HOST + ":3306/"
                + database_info.AUTHENTICATION_DATABASE_NAME + "?serverTimezone=UTC";

        try( Connection connection = DriverManager.getConnection( database_URI,
                                                                  database_info.AUTHENTICATION_DATABASE_USER,
                                                                  database_info.AUTHENTICATION_DATABASE_PASS ) )
        {
            Instant instant;
            instant = Instant.now()
                             .truncatedTo( ChronoUnit.SECONDS );
            String last_invalid_attempt = instant.toString()
                                                 .replace( 'T',
                                                           ' ' )
                                                 .replace( "Z",
                                                           "" );

            String query = "UPDATE `accounts` SET `last_invalid_attempt` = ?, `invalid_attempts` = ? WHERE `accounts`.`id` = ?";
            try( PreparedStatement prepared_statement = connection.prepareStatement( query ) )
            {
                prepared_statement.setString( 1,
                                              last_invalid_attempt );
                prepared_statement.setInt( 2,
                                           invalid_attempts );
                prepared_statement.setInt( 3,
                                           an_ID );
                prepared_statement.executeUpdate();
            }

            return instant;
        }
    }

    public static String resource_file_to_string( String path,
                                                  ServletContext servlet_context )
            throws IllegalArgumentException
    {
        try( InputStream input_stream = servlet_context.getResourceAsStream( "/WEB-INF/res/" + path );
                BufferedReader reader = new BufferedReader( new InputStreamReader( input_stream ) ); )
        {
            String line = "";
            StringBuffer string_buffer = new StringBuffer();
            while( ( line = reader.readLine() ) != null )
                string_buffer.append( line + System.lineSeparator() );
            return string_buffer.toString();
        }
        catch( Exception e )
        {
            throw new IllegalArgumentException( "Error reading file or invalid path" );
        }
    }

    public static String get_pom_properties( ServletContext servlet_context ) throws IllegalArgumentException
    {
        StringBuffer string_buffer = new StringBuffer();

        try( InputStream input_stream = servlet_context.getResourceAsStream( "/META-INF/maven/net.ddns.rkdawenterprises/ROOT/pom.properties" );
                BufferedReader reader = new BufferedReader( new InputStreamReader( input_stream ) ); )
        {
            String line = "";
            while( ( line = reader.readLine() ) != null )
                string_buffer.append( line + System.lineSeparator() );
        }
        catch( Exception e )
        {
            throw new IllegalArgumentException( "Error reading file or invalid path" );
        }

        try( InputStream input_stream = servlet_context.getResourceAsStream( "/META-INF/MANIFEST.MF" );
                BufferedReader reader = new BufferedReader( new InputStreamReader( input_stream ) ); )
        {
            String line = "";
            while( ( line = reader.readLine() ) != null )
                string_buffer.append( line + System.lineSeparator() );
        }
        catch( Exception e )
        {
            throw new IllegalArgumentException( "Error reading file or invalid path" );
        }

        return string_buffer.toString();
    }

    /**
     * An inelegant way of getting tomcat to reload the application.
     * 
     * @param servlet_context
     */
    public static void reload_application( ServletContext servlet_context )
    {
        System.out.println( "Reloading RKDAWE Web application..." );

        String context_path = servlet_context.getRealPath( "/" );
        File f = new File( context_path );
        String parent_path = f.getParent();
        try
        {
            FileUtils.touch( new File( parent_path + "/ROOT.war" ) );
        }
        catch( IOException exception )
        {
            System.out.println( exception );
        }
    }
}
