
package net.ddns.rkdawenterprises.rkdawe_api_common;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Weather_data
{
    public enum Type
    {
        LOOP,
        LOOP2,
        HILOWS,
    }

    public static final Gson m_GSON = new GsonBuilder().disableHtmlEscaping()
                                                       .setPrettyPrinting()
                                                       .create();

    public Weather_data()
    {
    }

    public void parse_packet( Type type,
                              byte[] packet,
                              int length )
            throws IllegalArgumentException, ArithmeticException
    {
        if( type == Type.LOOP2 )
        {
            if( length != 99 ) throw new IllegalArgumentException( "Incorrect packet size" );

            if( ( packet[0] != 'L' ) || ( packet[1] != 'O' ) || ( packet[2] != 'O' ) || ( packet[4] != 0x01 )
                    || ( packet[95] != 0x0A ) || ( packet[96] != 0x0D ) )
            {
                throw new IllegalArgumentException( "Invalid packet data" );
            }

            Check_CRC.check_CRC_16( packet,
                                    0,
                                    99 );

            int bar_trend_code = packet[3];
            switch( bar_trend_code )
            {
                case -60:
                    bar_trend = "Falling Rapidly";
                    break;
                case -20:
                    bar_trend = "Falling Slowly";
                    break;
                case 0:
                    bar_trend = "Steady";
                    break;
                case 20:
                    bar_trend = "Rising Slowly";
                    break;
                case 60:
                    bar_trend = "Rising Rapidly";
                    break;
                default:
                    bar_trend = "N/A";
                    break;
            }

            barometer = (double)bytes_to_short( packet,
                                                7 )
                    / 1000;
            inside_temperature = (double)bytes_to_short( packet,
                                                         9 )
                    / 10;
            inside_humidity = packet[11] & 0xFF;
            outside_temperature = (double)bytes_to_short( packet,
                                                          12 )
                    / 10;
            wind_speed = packet[14] & 0xFF;
            wind_direction = bytes_to_short( packet,
                                             16 );
            ten_min_avg_wind_speed = (double)bytes_to_short( packet,
                                                             18 )
                    / 10;
            two_min_avg_wind_speed = (double)bytes_to_short( packet,
                                                             20 )
                    / 10;
            ten_min_wind_gust = (double)bytes_to_short( packet,
                                                        22 )
                    / 10;
            wind_direction_of_ten_min_wind_gust = bytes_to_short( packet,
                                                                  24 );
            dew_point = bytes_to_short( packet,
                                        30 );
            outside_humidity = packet[33] & 0xFF;
            heat_index = bytes_to_short( packet,
                                         35 );
            wind_chill = bytes_to_short( packet,
                                         37 );
            rain_rate = (double)bytes_to_short( packet,
                                                41 )
                    / 100;
            storm_rain = (double)bytes_to_short( packet,
                                                 46 )
                    / 100;
            start_date_of_current_storm = bytes_to_date( packet,
                                                         48 );
            daily_rain = (double)bytes_to_short( packet,
                                                 50 )
                    / 100;
            last_fifteen_min_rain = (double)bytes_to_short( packet,
                                                            52 )
                    / 100;
            last_hour_rain = (double)bytes_to_short( packet,
                                                     54 )
                    / 100;
            daily_et = (double)bytes_to_short( packet,
                                               56 )
                    / 1000;
            last_twenty_four_hour_rain = (double)bytes_to_short( packet,
                                                                 58 )
                    / 100;

            return;
        }

        if( type == Type.LOOP )
        {
            if( length != 99 ) throw new IllegalArgumentException( "Incorrect packet size" );

            if( ( packet[0] != 'L' ) || ( packet[1] != 'O' ) || ( packet[2] != 'O' ) || ( packet[4] != 0x00 )
                    || ( packet[95] != 0x0A ) || ( packet[96] != 0x0D ) )
            {
                throw new IllegalArgumentException( "Invalid packet data" );
            }

            Check_CRC.check_CRC_16( packet,
                                    0,
                                    99 );

            month_rain = ( (double)bytes_to_short( packet,
                                                   52 )
                    / 100 );
            year_rain = ( (double)bytes_to_short( packet,
                                                  54 )
                    / 100 );

            int battery_status = packet[86] & 0xFF;
            transmitter_battery_status = ( ( battery_status & 0x01 ) == 0 ) ? "OK" : "LOW";
            console_battery_voltage = ( ( ( (double)( bytes_to_short( packet,
                                                                      87 ) )
                    * 300 ) / 512 ) / 100 );
        }

        if( type == Type.HILOWS )
        {
            if( length != 438 )
            {
                throw new IllegalArgumentException( "Incorrect packet size" );
            }

            Check_CRC.check_CRC_16( packet,
                                    0,
                                    438 );

            daily_low_barometer = (double)bytes_to_short( packet,
                                                          0 )
                    / 1000;
            daily_high_barometer = (double)bytes_to_short( packet,
                                                           2 )
                    / 1000;
            month_low_bar = (double)bytes_to_short( packet,
                                                    4 )
                    / 1000;
            month_high_bar = (double)bytes_to_short( packet,
                                                     6 )
                    / 1000;
            year_low_barometer = (double)bytes_to_short( packet,
                                                         8 )
                    / 1000;
            year_high_barometer = (double)bytes_to_short( packet,
                                                          10 )
                    / 1000;
            time_of_day_low_bar = bytes_to_time( packet,
                                                 12 );
            time_of_day_high_bar = bytes_to_time( packet,
                                                  14 );

            daily_hi_wind_speed = packet[16] & 0xFF;
            time_of_hi_speed = bytes_to_time( packet,
                                              17 );
            month_hi_wind_speed = packet[19] & 0xFF;
            year_hi_wind_speed = packet[20] & 0xFF;

            day_hi_inside_temp = (double)bytes_to_short( packet,
                                                         21 )
                    / 10;
            day_low_inside_temp = (double)bytes_to_short( packet,
                                                          23 )
                    / 10;
            time_day_hi_in_temp = bytes_to_time( packet,
                                                 25 );
            time_day_low_in_temp = bytes_to_time( packet,
                                                  27 );
            month_low_in_temp = (double)bytes_to_short( packet,
                                                        29 )
                    / 10;
            month_hi_in_temp = (double)bytes_to_short( packet,
                                                       31 )
                    / 10;
            year_low_in_temp = (double)bytes_to_short( packet,
                                                       33 )
                    / 10;
            year_hi_in_temp = (double)bytes_to_short( packet,
                                                      35 )
                    / 10;

            day_hi_in_hum = packet[37] & 0xFF;
            day_low_in_hum = packet[38] & 0xFF;
            time_day_hi_in_hum = bytes_to_time( packet,
                                                39 );
            time_day_low_in_hum = bytes_to_time( packet,
                                                 41 );
            month_hi_in_hum = packet[43] & 0xFF;
            month_low_in_hum = packet[44] & 0xFF;
            year_hi_in_hum = packet[45] & 0xFF;
            year_low_in_hum = packet[46] & 0xFF;

            day_low_out_temp = (double)bytes_to_short( packet,
                                                       47 )
                    / 10;
            day_hi_out_temp = (double)bytes_to_short( packet,
                                                      49 )
                    / 10;
            time_day_low_out_temp = bytes_to_time( packet,
                                                   51 );
            time_day_hi_out_temp = bytes_to_time( packet,
                                                  53 );
            month_hi_out_temp = (double)bytes_to_short( packet,
                                                        55 )
                    / 10;
            month_low_out_temp = (double)bytes_to_short( packet,
                                                         57 )
                    / 10;
            year_hi_out_temp = (double)bytes_to_short( packet,
                                                       59 )
                    / 10;
            year_low_out_temp = (double)bytes_to_short( packet,
                                                        61 )
                    / 10;

            day_low_dew_point = bytes_to_short( packet,
                                                63 );
            day_hi_dew_point = bytes_to_short( packet,
                                               65 );
            time_day_low_dew_point = bytes_to_time( packet,
                                                    67 );
            time_day_hi_dew_point = bytes_to_time( packet,
                                                   69 );
            month_hi_dew_point = bytes_to_short( packet,
                                                 71 );
            month_low_dew_point = bytes_to_short( packet,
                                                  73 );
            year_hi_dew_point = bytes_to_short( packet,
                                                75 );
            year_low_dew_point = bytes_to_short( packet,
                                                 77 );

            day_low_wind_chill = bytes_to_short( packet,
                                                 79 );
            time_day_low_chill = bytes_to_time( packet,
                                                81 );
            month_low_wind_chill = bytes_to_short( packet,
                                                   83 );
            year_low_wind_chill = bytes_to_short( packet,
                                                  85 );

            day_high_heat = bytes_to_short( packet,
                                            87 );
            time_of_day_high_heat = bytes_to_time( packet,
                                                   89 );
            month_high_heat = bytes_to_short( packet,
                                              91 );
            year_high_heat = bytes_to_short( packet,
                                             93 );

            day_high_rain_rate = (double)bytes_to_short( packet,
                                                         116 )
                    / 100;
            time_of_day_high_rain_rate = bytes_to_time( packet,
                                                        118 );
            hour_high_rain_rate = (double)bytes_to_short( packet,
                                                          120 )
                    / 100;
            month_high_rain_rate = (double)bytes_to_short( packet,
                                                           122 )
                    / 100;
            year_high_rain_rate = (double)bytes_to_short( packet,
                                                          124 )
                    / 100;

            day_low_humidity = packet[276] & 0xFF;
            day_hi_humidity = packet[284] & 0xFF;
            time_day_low_humidity = bytes_to_time( packet,
                                                   292 );
            time_day_hi_humidity = bytes_to_time( packet,
                                                  308 );
            month_hi_humidity = packet[324] & 0xFF;
            month_low_humidity = packet[332] & 0xFF;
            year_hi_humidity = packet[340] & 0xFF;
            year_low_humidity = packet[348] & 0xFF;
        }
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();

        builder.append( getClass().getName() );
        builder.append( "@" );
        builder.append( String.format( "0x%08X",
                                       hashCode() )
                + "\n" );
        builder.append( "[\n" );

        builder.append( "    system_name=" );
        builder.append( system_name );
        builder.append( ",\n    time=" );
        builder.append( time );
        builder.append( ",\n    heat_index_derived=" );
        builder.append( heat_index_derived );
        builder.append( ",\n    wind_chill_derived=" );
        builder.append( wind_chill_derived );

        builder.append( ",\n    wrd=" );
        builder.append( wrd );
        builder.append( ",\n    total_packets_received=" );
        builder.append( total_packets_received );
        builder.append( ",\n    total_packets_missed=" );
        builder.append( total_packets_missed );
        builder.append( ",\n    number_of_resynchronizations=" );
        builder.append( number_of_resynchronizations );
        builder.append( ",\n    largest_number_packets_received_in_a_row=" );
        builder.append( largest_number_packets_received_in_a_row );
        builder.append( ",\n    number_of_CRC_errors_detected=" );
        builder.append( number_of_CRC_errors_detected );
        builder.append( ",\n    firmware_date_code=" );
        builder.append( firmware_date_code );
        builder.append( ",\n    firmware_version=" );
        builder.append( firmware_version );
        builder.append( ",\n    DID=" );
        builder.append( DID );

        /*
         * Loop2 packet data.
         */
        builder.append( ",\n    bar_trend=" );
        builder.append( bar_trend );

        builder.append( ",\n    barometer=" );
        builder.append( barometer );
        builder.append( barometer_units );

        builder.append( ",\n    inside_temperature=" );
        builder.append( inside_temperature );
        builder.append( temperature_units );

        builder.append( ",\n    inside_humidity=" );
        builder.append( inside_humidity );
        builder.append( humidity_units );

        builder.append( ",\n    outside_temperature=" );
        builder.append( outside_temperature );
        builder.append( temperature_units );

        builder.append( ",\n    wind_speed=" );
        builder.append( wind_speed );
        builder.append( wind_speed_units );

        builder.append( ",\n    wind_direction=" );
        builder.append( wind_direction );
        builder.append( wind_direction_units );

        builder.append( ",\n    ten_min_avg_wind_speed=" );
        builder.append( ten_min_avg_wind_speed );
        builder.append( wind_speed_units );

        builder.append( ",\n    two_min_avg_wind_speed=" );
        builder.append( two_min_avg_wind_speed );
        builder.append( wind_speed_units );

        builder.append( ",\n    ten_min_wind_gust=" );
        builder.append( ten_min_wind_gust );
        builder.append( wind_speed_units );

        builder.append( ",\n    wind_direction_of_ten_min_wind_gust=" );
        builder.append( wind_direction_of_ten_min_wind_gust );
        builder.append( wind_direction_units );

        builder.append( ",\n    dew_point=" );
        builder.append( dew_point );
        builder.append( temperature_units );

        builder.append( ",\n    outside_humidity=" );
        builder.append( outside_humidity );
        builder.append( humidity_units );

        builder.append( ",\n    heat_index=" );
        builder.append( heat_index );
        builder.append( temperature_units );

        builder.append( ",\n    wind_chill=" );
        builder.append( wind_chill );
        builder.append( temperature_units );

        builder.append( ",\n    rain_rate=" );
        builder.append( rain_rate );
        builder.append( rain_rate_units );

        builder.append( ",\n    storm_rain=" );
        builder.append( storm_rain );
        builder.append( rain_units );

        builder.append( ",\n    start_date_of_current_storm=" );
        builder.append( start_date_of_current_storm );

        builder.append( ",\n    daily_rain=" );
        builder.append( daily_rain );
        builder.append( rain_units );

        builder.append( ",\n    last_fifteen_min_rain=" );
        builder.append( last_fifteen_min_rain );
        builder.append( rain_units );

        builder.append( ",\n    last_hour_rain=" );
        builder.append( last_hour_rain );
        builder.append( rain_units );

        builder.append( ",\n    daily_et=" );
        builder.append( daily_et );
        builder.append( rain_units );

        builder.append( ",\n    last_twenty_four_hour_rain=" );
        builder.append( last_twenty_four_hour_rain );
        builder.append( rain_units );

        /*
         * Loop packet data.
         */
        builder.append( ",\n    month_rain=" );
        builder.append( month_rain );
        builder.append( rain_units );

        builder.append( ",\n    year_rain=" );
        builder.append( year_rain );
        builder.append( rain_units );

        builder.append( ",\n    transmitter_battery_status=" );
        builder.append( transmitter_battery_status );

        builder.append( ",\n    console_battery_voltage=" );
        builder.append( console_battery_voltage );
        builder.append( console_battery_voltage_units );

        /*
         * HILOWS packet data.
         */
        builder.append( ",\n    daily_low_barometer=" );
        builder.append( daily_low_barometer );
        builder.append( barometer_units );

        builder.append( ",\n    daily_high_barometer=" );
        builder.append( daily_high_barometer );
        builder.append( barometer_units );

        builder.append( ",\n    month_low_bar=" );
        builder.append( month_low_bar );
        builder.append( barometer_units );

        builder.append( ",\n    month_high_bar=" );
        builder.append( month_high_bar );
        builder.append( barometer_units );

        builder.append( ",\n    year_low_barometer=" );
        builder.append( year_low_barometer );
        builder.append( barometer_units );

        builder.append( ",\n    year_high_barometer=" );
        builder.append( year_high_barometer );
        builder.append( barometer_units );

        builder.append( ",\n    time_of_day_low_bar=" );
        builder.append( time_of_day_low_bar );

        builder.append( ",\n    time_of_day_high_bar=" );
        builder.append( time_of_day_high_bar );

        builder.append( ",\n    daily_hi_wind_speed=" );
        builder.append( daily_hi_wind_speed );
        builder.append( wind_speed_units );

        builder.append( ",\n    time_of_hi_speed=" );
        builder.append( time_of_hi_speed );

        builder.append( ",\n    month_hi_wind_speed=" );
        builder.append( month_hi_wind_speed );
        builder.append( wind_speed_units );

        builder.append( ",\n    year_hi_wind_speed=" );
        builder.append( year_hi_wind_speed );
        builder.append( wind_speed_units );

        builder.append( ",\n    day_hi_inside_temp=" );
        builder.append( day_hi_inside_temp );
        builder.append( temperature_units );

        builder.append( ",\n    day_low_inside_temp=" );
        builder.append( day_low_inside_temp );
        builder.append( temperature_units );

        builder.append( ",\n    time_day_hi_in_temp=" );
        builder.append( time_day_hi_in_temp );

        builder.append( ",\n    time_day_low_in_temp=" );
        builder.append( time_day_low_in_temp );

        builder.append( ",\n    month_low_in_temp=" );
        builder.append( month_low_in_temp );
        builder.append( temperature_units );

        builder.append( ",\n    month_hi_in_temp=" );
        builder.append( month_hi_in_temp );
        builder.append( temperature_units );

        builder.append( ",\n    year_low_in_temp=" );
        builder.append( year_low_in_temp );
        builder.append( temperature_units );

        builder.append( ",\n    year_hi_in_temp=" );
        builder.append( year_hi_in_temp );
        builder.append( temperature_units );

        builder.append( ",\n    day_hi_in_hum=" );
        builder.append( day_hi_in_hum );
        builder.append( humidity_units );

        builder.append( ",\n    day_low_in_hum=" );
        builder.append( day_low_in_hum );
        builder.append( humidity_units );

        builder.append( ",\n    time_day_hi_in_hum=" );
        builder.append( time_day_hi_in_hum );

        builder.append( ",\n    time_day_low_in_hum=" );
        builder.append( time_day_low_in_hum );

        builder.append( ",\n    month_hi_in_hum=" );
        builder.append( month_hi_in_hum );
        builder.append( humidity_units );

        builder.append( ",\n    month_low_in_hum=" );
        builder.append( month_low_in_hum );
        builder.append( humidity_units );

        builder.append( ",\n    year_hi_in_hum=" );
        builder.append( year_hi_in_hum );
        builder.append( humidity_units );

        builder.append( ",\n    year_low_in_hum=" );
        builder.append( year_low_in_hum );
        builder.append( humidity_units );

        builder.append( ",\n    day_low_out_temp=" );
        builder.append( day_low_out_temp );
        builder.append( temperature_units );

        builder.append( ",\n    day_hi_out_temp=" );
        builder.append( day_hi_out_temp );
        builder.append( temperature_units );

        builder.append( ",\n    time_day_low_out_temp=" );
        builder.append( time_day_low_out_temp );

        builder.append( ",\n    time_day_hi_out_temp=" );
        builder.append( time_day_hi_out_temp );

        builder.append( ",\n    month_hi_out_temp=" );
        builder.append( month_hi_out_temp );
        builder.append( temperature_units );

        builder.append( ",\n    month_low_out_temp=" );
        builder.append( month_low_out_temp );
        builder.append( temperature_units );

        builder.append( ",\n    year_hi_out_temp=" );
        builder.append( year_hi_out_temp );
        builder.append( temperature_units );

        builder.append( ",\n    year_low_out_temp=" );
        builder.append( year_low_out_temp );
        builder.append( temperature_units );

        builder.append( ",\n    day_low_dew_point=" );
        builder.append( day_low_dew_point );
        builder.append( temperature_units );

        builder.append( ",\n    day_hi_dew_point=" );
        builder.append( day_hi_dew_point );
        builder.append( temperature_units );

        builder.append( ",\n    time_day_low_dew_point=" );
        builder.append( time_day_low_dew_point );

        builder.append( ",\n    time_day_hi_dew_point=" );
        builder.append( time_day_hi_dew_point );

        builder.append( ",\n    month_hi_dew_point=" );
        builder.append( month_hi_dew_point );
        builder.append( temperature_units );

        builder.append( ",\n    month_low_dew_point=" );
        builder.append( month_low_dew_point );
        builder.append( temperature_units );

        builder.append( ",\n    year_hi_dew_point=" );
        builder.append( year_hi_dew_point );
        builder.append( temperature_units );

        builder.append( ",\n    year_low_dew_point=" );
        builder.append( year_low_dew_point );
        builder.append( temperature_units );

        builder.append( ",\n    day_low_wind_chill=" );
        builder.append( day_low_wind_chill );
        builder.append( temperature_units );

        builder.append( ",\n    time_day_low_chill=" );
        builder.append( time_day_low_chill );

        builder.append( ",\n    month_low_wind_chill=" );
        builder.append( month_low_wind_chill );
        builder.append( temperature_units );

        builder.append( ",\n    year_low_wind_chill=" );
        builder.append( year_low_wind_chill );
        builder.append( temperature_units );

        builder.append( ",\n    day_high_heat=" );
        builder.append( day_high_heat );
        builder.append( temperature_units );

        builder.append( ",\n    time_of_day_high_heat=" );
        builder.append( time_of_day_high_heat );

        builder.append( ",\n    month_high_heat=" );
        builder.append( month_high_heat );
        builder.append( temperature_units );

        builder.append( ",\n    year_high_heat=" );
        builder.append( year_high_heat );
        builder.append( temperature_units );

        builder.append( ",\n    day_high_rain_rate=" );
        builder.append( day_high_rain_rate );
        builder.append( rain_rate_units );

        builder.append( ",\n    time_of_day_high_rain_rate=" );
        builder.append( time_of_day_high_rain_rate );

        builder.append( ",\n    hour_high_rain_rate=" );
        builder.append( hour_high_rain_rate );
        builder.append( rain_rate_units );

        builder.append( ",\n    month_high_rain_rate=" );
        builder.append( month_high_rain_rate );
        builder.append( rain_rate_units );

        builder.append( ",\n    year_high_rain_rate=" );
        builder.append( year_high_rain_rate );
        builder.append( rain_rate_units );

        builder.append( ",\n    day_low_humidity=" );
        builder.append( day_low_humidity );
        builder.append( humidity_units );

        builder.append( ",\n    day_hi_humidity=" );
        builder.append( day_hi_humidity );
        builder.append( humidity_units );

        builder.append( ",\n    time_day_low_humidity=" );
        builder.append( time_day_low_humidity );

        builder.append( ",\n    time_day_hi_humidity=" );
        builder.append( time_day_hi_humidity );

        builder.append( ",\n    month_hi_humidity=" );
        builder.append( month_hi_humidity );
        builder.append( humidity_units );

        builder.append( ",\n    month_low_humidity=" );
        builder.append( month_low_humidity );
        builder.append( humidity_units );

        builder.append( ",\n    year_hi_humidity=" );
        builder.append( year_hi_humidity );
        builder.append( humidity_units );

        builder.append( ",\n    year_low_humidity=" );
        builder.append( year_low_humidity );
        builder.append( humidity_units );

        builder.append( "\n]" );

        return builder.toString();
    }

    /**
     * The name of the weather station.
     */
    public String system_name = "Weather Station Donna @ Hot Springs, AR";

    /**
     * The time the weather data was retrieved as UTC.
     */
    public String time = "N/A";

    /**
     * Calculated heat-index and wind-chill.
     */
    public double heat_index_derived = Double.MAX_VALUE;
    public double wind_chill_derived = Double.MAX_VALUE;

    /**
     * Station information acquired during configuration.
     */
    public int wrd = Integer.MAX_VALUE;
    public int total_packets_received = Integer.MAX_VALUE;
    public int total_packets_missed = Integer.MAX_VALUE;
    public int number_of_resynchronizations = Integer.MAX_VALUE;
    public int largest_number_packets_received_in_a_row = Integer.MAX_VALUE;
    public int number_of_CRC_errors_detected = Integer.MAX_VALUE;
    public String firmware_date_code = "N/A";
    public String firmware_version = "N/A";
    public String DID = "N/A";

    /**
     * Measurement units
     */
    public String barometer_units = " in Hg";
    public String temperature_units = "&#x00B0;F";
    public String humidity_units = "%";
    public String wind_speed_units = " MPH";
    public String wind_direction_units = "&#x00B0;";
    public String rain_units = " in";
    public String rain_rate_units = " in/hr";
    public String bar_trend = "N/A";

    /**
     * Loop2 packet data.
     */
    public double barometer = Double.MAX_VALUE;
    public double inside_temperature = Double.MAX_VALUE;
    public int inside_humidity = Integer.MAX_VALUE;
    public double outside_temperature = Double.MAX_VALUE;
    public int wind_speed = Integer.MAX_VALUE;
    public int wind_direction = Integer.MAX_VALUE;
    public double ten_min_avg_wind_speed = Double.MAX_VALUE;
    public double two_min_avg_wind_speed = Double.MAX_VALUE;
    public double ten_min_wind_gust = Double.MAX_VALUE;
    public int wind_direction_of_ten_min_wind_gust = Integer.MAX_VALUE;
    public int dew_point = Integer.MAX_VALUE;
    public int outside_humidity = Integer.MAX_VALUE;
    public int heat_index = Integer.MAX_VALUE;
    public int wind_chill = Integer.MAX_VALUE;
    public double rain_rate = Double.MAX_VALUE;
    public double storm_rain = Double.MAX_VALUE;
    public String start_date_of_current_storm = "N/A";
    public double daily_rain = Double.MAX_VALUE;
    public double last_fifteen_min_rain = Double.MAX_VALUE;
    public double last_hour_rain = Double.MAX_VALUE;
    public double daily_et = Double.MAX_VALUE;
    public double last_twenty_four_hour_rain = Double.MAX_VALUE;

    /**
     * Loop packet data.
     */
    public double month_rain = Double.MAX_VALUE;
    public double year_rain = Double.MAX_VALUE;
    public String transmitter_battery_status = "N/A";
    public double console_battery_voltage = Double.MAX_VALUE;
    public String console_battery_voltage_units = " Volts";

    /**
     * HILOWS packet data.
     */
    public double daily_low_barometer = Double.MAX_VALUE;
    public double daily_high_barometer = Double.MAX_VALUE;
    public double month_low_bar = Double.MAX_VALUE;
    public double month_high_bar = Double.MAX_VALUE;
    public double year_low_barometer = Double.MAX_VALUE;
    public double year_high_barometer = Double.MAX_VALUE;
    public String time_of_day_low_bar = "N/A";
    public String time_of_day_high_bar = "N/A";

    public int daily_hi_wind_speed = Integer.MAX_VALUE;
    public String time_of_hi_speed = "N/A";
    public int month_hi_wind_speed = Integer.MAX_VALUE;
    public int year_hi_wind_speed = Integer.MAX_VALUE;

    public double day_hi_inside_temp = Double.MAX_VALUE;
    public double day_low_inside_temp = Double.MAX_VALUE;
    public String time_day_hi_in_temp = "N/A";
    public String time_day_low_in_temp = "N/A";
    public double month_low_in_temp = Double.MAX_VALUE;
    public double month_hi_in_temp = Double.MAX_VALUE;
    public double year_low_in_temp = Double.MAX_VALUE;
    public double year_hi_in_temp = Double.MAX_VALUE;

    public int day_hi_in_hum = Integer.MAX_VALUE;
    public int day_low_in_hum = Integer.MAX_VALUE;
    public String time_day_hi_in_hum = "N/A";
    public String time_day_low_in_hum = "N/A";
    public int month_hi_in_hum = Integer.MAX_VALUE;
    public int month_low_in_hum = Integer.MAX_VALUE;
    public int year_hi_in_hum = Integer.MAX_VALUE;
    public int year_low_in_hum = Integer.MAX_VALUE;

    public double day_low_out_temp = Double.MAX_VALUE;
    public double day_hi_out_temp = Double.MAX_VALUE;
    public String time_day_low_out_temp = "N/A";
    public String time_day_hi_out_temp = "N/A";
    public double month_hi_out_temp = Double.MAX_VALUE;
    public double month_low_out_temp = Double.MAX_VALUE;
    public double year_hi_out_temp = Double.MAX_VALUE;
    public double year_low_out_temp = Double.MAX_VALUE;

    public int day_low_dew_point = Integer.MAX_VALUE;
    public int day_hi_dew_point = Integer.MAX_VALUE;
    public String time_day_low_dew_point = "N/A";
    public String time_day_hi_dew_point = "N/A";
    public int month_hi_dew_point = Integer.MAX_VALUE;
    public int month_low_dew_point = Integer.MAX_VALUE;
    public int year_hi_dew_point = Integer.MAX_VALUE;
    public int year_low_dew_point = Integer.MAX_VALUE;

    public int day_low_wind_chill = Integer.MAX_VALUE;
    public String time_day_low_chill = "N/A";
    public int month_low_wind_chill = Integer.MAX_VALUE;
    public int year_low_wind_chill = Integer.MAX_VALUE;

    public int day_high_heat = Integer.MAX_VALUE;
    public String time_of_day_high_heat = "N/A";
    public int month_high_heat = Integer.MAX_VALUE;
    public int year_high_heat = Integer.MAX_VALUE;

    public double day_high_rain_rate = Double.MAX_VALUE;
    public String time_of_day_high_rain_rate = "N/A";
    public double hour_high_rain_rate = Double.MAX_VALUE;
    public double month_high_rain_rate = Double.MAX_VALUE;
    public double year_high_rain_rate = Double.MAX_VALUE;

    public int day_low_humidity = Integer.MAX_VALUE;
    public int day_hi_humidity = Integer.MAX_VALUE;
    public String time_day_low_humidity = "N/A";
    public String time_day_hi_humidity = "N/A";
    public int month_hi_humidity = Integer.MAX_VALUE;
    public int month_low_humidity = Integer.MAX_VALUE;
    public int year_hi_humidity = Integer.MAX_VALUE;
    public int year_low_humidity = Integer.MAX_VALUE;

    /**
     * Converts two bytes to a signed short value.
     *
     * @param buffer The byte buffer containing the two bytes.
     * @param index  The index within the buffer to convert.
     *
     * @return The converted value.
     */
    public static short bytes_to_short( byte[] buffer,
                                        int index )
    {
        return (short)( ( ( buffer[index + 1] & 0xFF ) << 8 ) | ( buffer[index] & 0xFF ) );
    }

    String bytes_to_date( byte[] buffer,
                          int index )
    {
        short bits = bytes_to_short( buffer,
                                     index );
        int month = ( bits & 0b0_1111_0000_0000_0000 ) >>> 12;
        int day = ( bits & 0b0_0000_1111_1000_0000 ) >>> 7;
        int year = ( bits & 0b0_0000_0000_0111_1111 ) + 2000;
        return( Integer.toString( year ) + "-" + Integer.toString( month ) + "-" + Integer.toString( day ) );
    }

    String bytes_to_time( byte[] buffer,
                          int index )
    {
        short bits = bytes_to_short( buffer,
                                     index );
        double hours = Math.floor( bits / 100 );
        double minutes = bits % 100;

        ZonedDateTime time_at_local = LocalDate.now()
                                               .atStartOfDay()
                                               .plusHours( (long)hours )
                                               .plusMinutes( (long)minutes )
                                               .atZone( TimeZone.getDefault()
                                                                .toZoneId() );

        ZonedDateTime time_at_zulu = ZonedDateTime.ofInstant( time_at_local.toInstant(),
                                                              ZoneId.of( "UTC" ) );

        return( DateTimeFormatter.ofPattern( "yyyy-MM-dd'T'HH:mm:ss'Z'" )
                                 .format( time_at_zulu ) );
    }

    public static String get_history_record_columns()
    {
        return( "time,barometer,inside_temperature,inside_humidity,outside_temperature,wind_speed,wind_direction,two_min_avg_wind_speed,ten_min_wind_gust,wind_direction_of_ten_min_wind_gust,dew_point,outside_humidity,heat_index,wind_chill,rain_rate,storm_rain,last_fifteen_min_rain,last_hour_rain,last_twenty_four_hour_rain\n" );
    }

    public String get_history_record()
    {
        StringBuilder builder = new StringBuilder();

        builder.append( time );
        builder.append( "," );
        builder.append( barometer );
        builder.append( "," );
        builder.append( inside_temperature );
        builder.append( "," );
        builder.append( inside_humidity );
        builder.append( "," );
        builder.append( outside_temperature );
        builder.append( "," );
        builder.append( wind_speed );
        builder.append( "," );
        builder.append( wind_direction );
        builder.append( "," );
        builder.append( two_min_avg_wind_speed );
        builder.append( "," );
        builder.append( ten_min_wind_gust );
        builder.append( "," );
        builder.append( wind_direction_of_ten_min_wind_gust );
        builder.append( "," );
        builder.append( dew_point );
        builder.append( "," );
        builder.append( outside_humidity );
        builder.append( "," );
        builder.append( heat_index );
        builder.append( "," );
        builder.append( wind_chill );
        builder.append( "," );
        builder.append( rain_rate );
        builder.append( "," );
        builder.append( storm_rain );
        builder.append( "," );
        builder.append( last_fifteen_min_rain );
        builder.append( "," );
        builder.append( last_hour_rain );
        builder.append( "," );
        builder.append( last_twenty_four_hour_rain );

        builder.append( "\n" );

        return builder.toString();
    }

    public static String serialize_to_JSON( Weather_data object )
    {
        return m_GSON.toJson( object );
    }

    public static Weather_data deserialize_from_JSON( String string_JSON )
    {
        Weather_data object = null;
        try
        {
            object = m_GSON.fromJson( string_JSON,
                                      Weather_data.class );
        }
        catch( com.google.gson.JsonSyntaxException exception )
        {
            System.out.println( "Bad data format for Weather_data: " + exception );
            System.out.println( ">>>" + string_JSON + "<<<" );
        }

        return object;
    }

    public String serialize_to_JSON()
    {
        return serialize_to_JSON( this );
    }

    /**
     * Calculates the heat index according to the US National Weather Service.
     * 
     * @param temperature The surface air temperature in degrees Fahrenheit.
     * @param humidity The relative humidity in percent.
     * @return The heat index.
     */
    public static double calculate_heat_index(double temperature, double humidity)
    {
        if(temperature <= 40) return temperature;

        double heat_index = 0.5 * (temperature + 61 + ((temperature - 68) * 1.2) + (humidity * 0.094));

        if(heat_index > 79)
        {
            heat_index = -42.379
                + (2.04901523 * temperature)
                + (10.14333127 * humidity)
                - (0.22475541 * temperature * humidity)
                - (0.00683783 * temperature * temperature)
                - (0.05481717 * humidity * humidity)
                + (0.00122874 * temperature * temperature * humidity)
                + (0.00085282 * temperature * humidity * humidity)
                - (0.00000199 * temperature * temperature * humidity * humidity);

            if((humidity < 13) && (temperature >= 80) && (temperature <= 112))
            {
                heat_index -= ((13 - humidity) / 4)
                    * Math.sqrt((17 - Math.abs(temperature - 95)) / 17);
            }
            else if((humidity > 85) && (temperature >= 80) && (temperature <= 87))
            {
                heat_index += ((humidity - 85) / 10) * ((87 - temperature) / 5);
            }
        }

        return heat_index;
    }

    /**
     * Calculates the wind chill according to the US National Weather Service.
     * 
     * @param temperature The surface air temperature in degrees Fahrenheit.
     * @param wind_speed The surface wind speed in MPH.
     * @return
     */
    public static double calculate_wind_chill(double temperature, double wind_speed)
    {
        if((temperature > 50) || (wind_speed <= 3)) return temperature;

        return 35.74 + (0.6215 * temperature)
            - (35.75 * Math.pow(wind_speed, 0.16))
            + (0.4275 * temperature * Math.pow(wind_speed, 0.16));
    }
}
