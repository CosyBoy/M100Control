package com.dji.FPVDemo;

/**
 * Created by Lenovo on 2018/12/19.
 */

public class UavStatusInfo {
    private String drone_id;
    private int connect_status,charge_status;
    private int charge,voltage,current;
    private float temperature;
    private double longitude,latitude;
    private boolean isflying;
    private float altitude;
    private float velocity_x,velocity_y,velocity_z;
    private  float text_gimbal_roll;

    public void setVelocityX(float velocity_x) {this.velocity_x=velocity_x;}
    public float getVelocityX() {return  velocity_x;}

    public void setVelocityY(float velocity_y) {this.velocity_y=velocity_y;}
    public float getVelocityY() {return  velocity_y;}

    public void setVelocityZ(float velocity_z) {this.velocity_z=velocity_z;}
    public float getVelocityZ() {return  velocity_z;}

    public void setRollFineTuneInDegrees(float text_gimbal_roll) {this.text_gimbal_roll=text_gimbal_roll;}
    public  float getRollFineTuneInDegrees() {return text_gimbal_roll;}

    public float getAltitude() {
        return altitude;
    }

    public void setAltitude(float altitude) {
        this.altitude = altitude;
    }

    public boolean getIsflying() {
        return isflying;
    }

    public void setIsflying(boolean isflying) {
        this.isflying = isflying;
    }

    public void setDrone_id(String drone_id){
        this.drone_id = drone_id;
    }

    public String getDrone_id() {
        return drone_id;
    }

    public void setTemperature(float tem) {
        this.temperature = tem;
    }

    public float getTemperature(){
        return temperature;
    }

    public void setCharge(int ch) {
        this.charge = ch;
    }

    public int getCharge(){
        return charge;
    }

    public int getConnect_status() {
        return connect_status;
    }

    public void setConnect_status(int con) {
        this.connect_status = con;
    }

    public void setLatitude(double lat) {
        this.latitude = lat;
    }

    public double getLatitude(){
        return latitude;
    }

    public void setLongitude(double lon) {
        this.longitude = lon;
    }

    public double getLongitude(){
        return longitude;
    }

    public void setVoltage(int vol) {
        this.voltage = vol;
    }

    public int getVoltage(){
        return voltage;
    }

    public void setCurrent(int cur) {
        this.current = cur;
    }

    public int getCurrent(){
        return current;
    }

    public int getCharge_status() {
        return charge_status;
    }

    public void setCharge_status(int cs) {
        this.charge_status = cs;
    }

}
