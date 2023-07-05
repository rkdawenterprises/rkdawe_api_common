
/*
 * Copyright (c) 2019-2023 RKDAW Enterprises and Ralph Williamson.
 *       email: rkdawenterprises@gmail.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.ddns.rkdawenterprises.rkdawe_api_common;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalField;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.apache.commons.codec.binary.Hex;

public class Utilities
{
    public static String hash_password( String password,
                                        byte[] salt )
            throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        int iterations = 10000;
        int key_length = 512;
        char[] password_chars = password.toCharArray();
        byte[] hashed_bytes = hash_PBKDF2( password_chars,
                                           salt,
                                           iterations,
                                           key_length );
        return Hex.encodeHexString( hashed_bytes );
    }

    public static byte[] hash_PBKDF2( char[] message,
                                      byte[] salt,
                                      int iterations,
                                      int key_length )
            throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        SecretKeyFactory secret_key_factory = SecretKeyFactory.getInstance( "PBKDF2WithHmacSHA512" );
        PBEKeySpec key_spec = new PBEKeySpec( message,
                                              salt,
                                              iterations,
                                              key_length );
        SecretKey key = secret_key_factory.generateSecret( key_spec );
        return key.getEncoded();
    }

    /**
     *
     * @param key_length The length of the key to use.
     *
     * @return The public and private keys, Base64 encoded, in a string array. Index
     *         zero is the private key and index one is the public key.
     *
     * @throws NoSuchAlgorithmException
     */
    public static String[] generate_RSA_key_pair_base64( int key_length ) throws NoSuchAlgorithmException
    {
        KeyPairGenerator key_pair_generator = KeyPairGenerator.getInstance( "RSA" );
        SecureRandom secure_random = SecureRandom.getInstance( "SHA1PRNG" );
        key_pair_generator.initialize( key_length,
                                       secure_random );
        KeyPair key_pair = key_pair_generator.generateKeyPair();
        PrivateKey private_key = key_pair.getPrivate();
        PublicKey public_key = key_pair.getPublic();
        String[] return_pair = new String[2];
        return_pair[0] = Base64.getEncoder()
                               .encodeToString( private_key.getEncoded() );
        return_pair[1] = Base64.getEncoder()
                               .encodeToString( public_key.getEncoded() );
        return return_pair;
    }

    public static PrivateKey convert_base64_private_key_to_PKCS8( String base64PrivateKey )
            throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        PKCS8EncodedKeySpec a_PKCS8_encoded_key_spec = new PKCS8EncodedKeySpec( Base64.getDecoder()
                                                                                      .decode( base64PrivateKey.getBytes() ) );
        return KeyFactory.getInstance( "RSA" )
                         .generatePrivate( a_PKCS8_encoded_key_spec );
    }

    public static String decrypt_RSA( byte[] data,
                                      PrivateKey privateKey )
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException,
            IllegalBlockSizeException
    {
        Cipher cipher = Cipher.getInstance( "RSA" );
        cipher.init( Cipher.DECRYPT_MODE,
                     privateKey );
        return new String( cipher.doFinal( data ) );
    }

    public static String decrypt_RSA_base64( String base64data,
                                             String base64PrivateKey )
            throws IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidKeySpecException
    {
        return decrypt_RSA( Base64.getDecoder()
                                  .decode( base64data.getBytes() ),
                            convert_base64_private_key_to_PKCS8( base64PrivateKey ) );
    }

    public static Date add_seconds( Date date,
                                    Integer seconds )
    {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime( date );
        calendar.add( Calendar.SECOND,
                      seconds );

        return calendar.getTime();
    }

    public enum ROUNDING_TYPE
    {
        DOWN,
        UP,
        HALF_UP
    }

    public static ZonedDateTime round( ZonedDateTime zoned_date_time,
                                       TemporalField round_to_field,
                                       int rounding_increment )
    {
        return round( zoned_date_time,
                      round_to_field,
                      rounding_increment,
                      ROUNDING_TYPE.HALF_UP );
    }

    public static ZonedDateTime round( ZonedDateTime zoned_date_time,
                                       TemporalField round_to_field,
                                       int rounding_increment,
                                       ROUNDING_TYPE type )
    {
        long field_value = zoned_date_time.getLong( round_to_field );
        long r = field_value % rounding_increment;
        ZonedDateTime ceiling = zoned_date_time.plus( rounding_increment - r,
                                                      round_to_field.getBaseUnit() )
                                               .truncatedTo( round_to_field.getBaseUnit() );
        ZonedDateTime floor = zoned_date_time.plus( -r,
                                                    round_to_field.getBaseUnit() )
                                             .truncatedTo( round_to_field.getBaseUnit() );

        if( type == ROUNDING_TYPE.DOWN )
        {
            return floor;
        }
        else if( type == ROUNDING_TYPE.UP )
        {
            return ceiling;
        }
        else
        {
            Duration distance_to_floor = Duration.between( floor,
                                                           zoned_date_time );
            Duration distance_to_ceiling = Duration.between( zoned_date_time,
                                                             ceiling );
            ZonedDateTime rounded = distance_to_floor.compareTo( distance_to_ceiling ) < 0 ? floor : ceiling;
            return rounded;
        }
    }

    public static final Duration DEFAULT_NETWORK_TIMEOUT = Duration.ofSeconds( 5 );

    /**
     * Sends a UDP broadcast message and waits for a response. Only waits for a
     * single response from a single responder. Uses the
     * {@link #DEFAULT_NETWORK_TIMEOUT}.
     *
     * @param message         The message data to broadcast.
     * @param port            The UDP port to broadcast on.
     * @param datagram_packet Container for the response to the broadcast.
     *
     * @throws SocketException
     * @throws UnknownHostException
     * @throws IllegalArgumentException
     * @throws SecurityException
     * @throws IOException
     * @throws SocketTimeoutException
     */
    public static void send_UDP_broadcast( byte[] message,
                                           int port,
                                           DatagramPacket receive_datagram_packet )
            throws SocketException, UnknownHostException, IllegalArgumentException, SecurityException, IOException,
            SocketTimeoutException
    {
        send_UDP_broadcast( message,
                            port,
                            receive_datagram_packet,
                            DEFAULT_NETWORK_TIMEOUT );
    }

    public static String get_local_IP_address() throws UnknownHostException
    {
        return InetAddress.getLocalHost()
                          .getHostAddress();
    }

    public static InetAddress get_broadcast_address( String local_IP_address )
            throws UnknownHostException, SocketException
    {
        InetAddress address = InetAddress.getByName( local_IP_address );
        NetworkInterface network_interface = NetworkInterface.getByInetAddress( address );

        if( network_interface.isUp() && !network_interface.isLoopback() )
        {
            List< InterfaceAddress > interface_addresses = network_interface.getInterfaceAddresses();
            for( InterfaceAddress interface_address : interface_addresses )
            {
                String host = interface_address.getAddress()
                                               .getHostAddress();
                InetAddress broadcast = interface_address.getBroadcast();
                if( host.equals( local_IP_address ) && broadcast != null )
                {
                    return broadcast;
                }
            }
        }

        return null;
    }

    /**
     * Sends a UDP broadcast message and waits for a response. Only waits for a
     * single response from a single responder.
     *
     * @param message         The message data to broadcast.
     * @param port            The UDP port to broadcast on.
     * @param timeout         The socket timeout in milliseconds.
     * @param datagram_packet Container for the response to the broadcast. Returns
     *                        the first response received that is not from
     *                        localhost.
     *
     * @throws SocketException
     * @throws UnknownHostException
     * @throws IllegalArgumentException
     * @throws SecurityException
     * @throws IOException
     * @throws SocketTimeoutException
     */
    public static void send_UDP_broadcast( byte[] message,
                                           int port,
                                           DatagramPacket receive_datagram_packet,
                                           Duration timeout )
            throws SocketException, UnknownHostException, IllegalArgumentException, SecurityException, IOException,
            SocketTimeoutException
    {
        String local_IP_address = get_local_IP_address();
        InetAddress broadcast_address = get_broadcast_address( local_IP_address );
        if( broadcast_address == null ) throw new IllegalArgumentException( "Could not get broadcast address" );

        try( DatagramSocket dsock = new DatagramSocket( port ) )
        {
            dsock.setSoTimeout( (int)timeout.toMillis() );
            dsock.setBroadcast( true );
            DatagramPacket transmit_datagram_packet = new DatagramPacket( message,
                                                                          message.length,
                                                                          broadcast_address,
                                                                          port );
            dsock.send( transmit_datagram_packet );

            while( true )
            {
                dsock.receive( receive_datagram_packet );
                String received_from = receive_datagram_packet.getAddress()
                                                              .getHostAddress();
                if( !received_from.equals( local_IP_address ) ) break;
            }
        }
    }

    /**
     * Returns a new byte array that is a subbytearray of the given byte array. The
     * subbytearray begins at the specified begin_index and extends to the byte at
     * the index of ( end_index - 1 ). Thus the length of the subbytearray is (
     * end_index - begin_index ).
     *
     * @param begin_index The beginning index, inclusive.
     * @param end_index   The ending index, exclusive.
     *
     * @return The specified subbytearray.
     *
     * @throws IndexOutOfBoundsException If begin_index or end_index are negative,
     *                                   or if end_index is greater than source
     *                                   length, or if begin_index is greater than
     *                                   end_index.
     */
    public static byte[] subbytearray( byte[] source,
                                       int begin_index,
                                       int end_index )
            throws IndexOutOfBoundsException
    {
        if( ( begin_index < 0 ) || ( end_index < 0 ) || ( end_index > source.length ) || ( begin_index > begin_index ) )
        {
            throw new IndexOutOfBoundsException();
        }

        byte[] destination = new byte[end_index - begin_index];
        System.arraycopy( source,
                          begin_index,
                          destination,
                          0,
                          end_index - begin_index );
        return destination;
    }

    public static void print_buffer( byte[] buffer )
    {
        print_buffer( buffer,
                      buffer.length,
                      16 );
    }

    public static void print_buffer( byte[] buffer,
                                     int size )
    {
        print_buffer( buffer,
                      size,
                      16 );
    }

    public static String repeat( String string, int count )
    {
        if( ( string.length() == 0 ) || ( count == 0 ) ) return string;
        String string_repeated = "";
        for( int i = 0; i < count; i++ ) string_repeated += string;
        return string_repeated;
    }

    public static void print_buffer( byte[] buffer,
                                     int size,
                                     int pitch )
    {
        if( size > 0xFFFF ) return;

        System.out.printf( ">> %d bytes:%n",
                           size );

        String line_chars = "";

        int i;
        for( i = 0; i < size; i++ )
        {
            if( i % pitch == 0 )
            {
                if( i != 0 )
                {
                    if( line_chars.length() > 0 ) System.out.printf( "\"%s\"%n",
                                                                     line_chars );
                    line_chars = "";
                }

                System.out.printf( "    %04X ",
                                   i );
            }

            System.out.printf( "%02X ",
                               buffer[i] );
            line_chars += Character.isISOControl( (char)buffer[i] ) ? "." : Character.toString( (char)buffer[i] );
        }

        int x = i % pitch;
        if( x != 0 )
        {
            if( line_chars.length() > 0 ) System.out.printf( "%s\"%s\"%n",
                                                             repeat("   ", pitch - x ),
                                                             line_chars );
            line_chars = "";
        }
        else
        {
            if( line_chars.length() > 0 ) System.out.printf( "\"%s\"%n",
                                                             line_chars );
        }
    }

    /**
     * Currently does not work with negative durations.
     * 
     * @param duration
     * 
     * @return
     */
    public static String duration_to_string( Duration duration )
    {
        long seconds_per_year = 31556952;
        long years = duration.getSeconds() / seconds_per_year;
        duration = duration.minusSeconds( years * seconds_per_year );

        long seconds_per_month = 2629746;
        long months = duration.getSeconds() / seconds_per_month;
        duration = duration.minusSeconds( months * seconds_per_month );

        long seconds_per_week = 604800;
        long weeks = duration.getSeconds() / seconds_per_week;
        duration = duration.minusSeconds( weeks * seconds_per_week );

        long seconds_per_day = 86400;
        long days = duration.getSeconds() / seconds_per_day;
        duration = duration.minusSeconds( days * seconds_per_day );

        long seconds_per_hour = 3600;
        long hours = duration.getSeconds() / seconds_per_hour;
        duration = duration.minusSeconds( hours * seconds_per_hour );

        long seconds_per_minute = 60;
        long minutes = duration.getSeconds() / seconds_per_minute;
        duration = duration.minusSeconds( minutes * seconds_per_minute );

        long seconds = duration.getSeconds();
        int nanoseconds = duration.getNano();

        StringBuilder duration_HMS_string_builder = new StringBuilder();

        if( hours > 0 )
        {
            duration_HMS_string_builder.append( hours );
            duration_HMS_string_builder.append( "h" );
        }

        if( ( duration_HMS_string_builder.length() > 0 ) && ( minutes > 0 ) ) duration_HMS_string_builder.append( ":" );

        if( minutes > 0 )
        {
            duration_HMS_string_builder.append( minutes );
            duration_HMS_string_builder.append( "m" );
        }

        if( ( duration_HMS_string_builder.length() > 0 ) && ( ( seconds > 0 ) || ( nanoseconds > 0 ) ) )
            duration_HMS_string_builder.append( ":" );

        if( seconds > 0 )
        {
            duration_HMS_string_builder.append( seconds );
        }

        if( nanoseconds > 0 )
        {
            double seconds_fraction = (double)nanoseconds / 1000000000;
            DecimalFormat formatter = new DecimalFormat( "#.0########" );
            duration_HMS_string_builder.append( formatter.format( seconds_fraction ) );
        }

        if( ( seconds > 0 ) || ( nanoseconds > 0 ) ) duration_HMS_string_builder.append( "s" );

        StringBuilder duration_string_builder = new StringBuilder( 64 );

        if( years > 0 )
        {
            duration_string_builder.append( years );
            duration_string_builder.append( "y" );
        }

        if( ( duration_string_builder.length() > 0 ) && ( months > 0 ) ) duration_string_builder.append( ", " );

        if( months > 0 )
        {
            duration_string_builder.append( months );
            duration_string_builder.append( "mo" );
        }

        if( ( duration_string_builder.length() > 0 ) && ( weeks > 0 ) ) duration_string_builder.append( ", " );

        if( weeks > 0 )
        {
            duration_string_builder.append( weeks );
            duration_string_builder.append( "w" );
        }

        if( ( duration_string_builder.length() > 0 ) && ( days > 0 ) ) duration_string_builder.append( ", " );

        if( days > 0 )
        {
            duration_string_builder.append( days );
            duration_string_builder.append( "d" );
        }

        if( ( duration_string_builder.length() > 0 ) && ( duration_HMS_string_builder.length() > 0 ) )
            duration_string_builder.append( ", " );

        if( duration_HMS_string_builder.length() > 0 ) duration_string_builder.append( duration_HMS_string_builder );

        return duration_string_builder.toString();
    }

    public static void sleep( Duration duration )
    {
        try
        {
            Thread.sleep( duration.toMillis() );
        }
        catch( InterruptedException exception )
        {
        }
    }

    /**
     * Equivalent to GNU Broken-down Time structure.
     */
    public static final class tm
    {
        public int tm_sec;
        public int tm_min;
        public int tm_hour;
        public int tm_mday;
        public int tm_mon;
        public int tm_year;
        public int tm_wday;
        public int tm_yday;
        public Boolean tm_isdst;
        public long tm_gmtoff;
        public String tm_zone;
    }

    public static tm get_local_tm()
    {
        ZonedDateTime system_now = ZonedDateTime.now();

        tm local_tm = new tm();
        local_tm.tm_gmtoff = system_now.getOffset()
                                       .getTotalSeconds();
        local_tm.tm_zone = system_now.getZone()
                                     .getId();
        local_tm.tm_isdst = system_now.getZone()
                                      .getRules()
                                      .isDaylightSavings( system_now.toInstant() );
        local_tm.tm_sec = system_now.getSecond();
        local_tm.tm_min = system_now.getMinute();
        local_tm.tm_hour = system_now.getHour();
        local_tm.tm_mday = system_now.getDayOfMonth();
        local_tm.tm_mon = system_now.getMonthValue() - 1;
        local_tm.tm_year = system_now.getYear() - 1900;
        local_tm.tm_wday = system_now.getDayOfWeek()
                                     .getValue();
        if( local_tm.tm_wday == 7 ) local_tm.tm_wday = 0;
        local_tm.tm_yday = system_now.getDayOfYear() - 1;

        return local_tm;
    }

    public static String convert_time_UTC_to_local( String time_UTC, String pattern )
    {
        return DateTimeFormatter.ofPattern( pattern )
            .format( convert_time_UTC_to_local( time_UTC ) );
    }

    public static ZonedDateTime convert_time_UTC_to_local( String time_UTC )
    {
        return ZonedDateTime.parse(time_UTC).withZoneSameInstant(ZoneId.of(TimeZone.getDefault().getID()));
    }

    public static ZonedDateTime convert_timestamp_to_local(long timestamp_ms, String zone)
    {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp_ms), ZoneId.of(zone));
    }

    public static ZonedDateTime convert_timestamp_to_UTC(long timestamp_ms, String zone)
    {
        return (ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp_ms), ZoneId.of(zone)))
                .withZoneSameInstant(ZoneId.of("UTC"));
    }

    public static String convert_timestamp_to_local( long timestamp_ms,
                                                     String zone,
                                                     String pattern)
    {
        return DateTimeFormatter.ofPattern( pattern )
                .format(convert_timestamp_to_local( timestamp_ms,
                        zone));
    }
}
