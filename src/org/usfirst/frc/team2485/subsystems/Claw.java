package org.usfirst.frc.team2485.subsystems;

import org.usfirst.frc.team2485.util.ThresholdHandler;

import edu.wpi.first.wpilibj.*;

/**
 * @author Aidan Fay
 * @author Ben Clark
 */

public class Claw {
	
	private static final double LOWEST_POS = 100;
	private static final double POS_RANGE = 400;
	
	private double kP = 0.005, kI = 0, kD = 0;
	private VictorSP winchMotor;
	private Solenoid actuator;
	private AnalogPotentiometer pot;
	
	private PIDController elevationPID;
	
	private boolean automatic = true;

	public Claw(VictorSP winchMotor, Solenoid actuator, AnalogPotentiometer pot) {
		this.winchMotor = winchMotor;
		this.actuator 	= actuator;
		this.pot 		= pot;
		
		elevationPID = new PIDController(kP, kI, kD, pot, winchMotor);
	}
	
	public Claw(int winchMotorPort, int actuatorPort, int potPort){
		this(new VictorSP(winchMotorPort), new Solenoid(actuatorPort), new AnalogPotentiometer(potPort));
	}
	
	public void open() {
		actuator.set(true);
	}
	
	public void close() {
		actuator.set(false);
	}
	
	public boolean isOpen() {
		return actuator.get();
	}
	
	/**
	 * If in manual mode this method will manually control the winch and return true.
	 * If in automatic mode this method will do nothing, and return false.
	 * @param speed
	 * @return
	 */
	public void liftManually(double speed) {
		setManual();
		double adjustedSpeed = ThresholdHandler.handleThreshold(speed, 0.1)/4;
		System.out.println(speed + " | " + adjustedSpeed);
		if (adjustedSpeed > 1){
			adjustedSpeed = 1;
		} else if (adjustedSpeed < -1){
			adjustedSpeed = -1;
		}
		winchMotor.set(adjustedSpeed);
	}
	
	public void setPID(double kP, double kI, double kD) {
		this.kP = kP;
		this.kI = kI;
		this.kD = kD;
		
		elevationPID.setPID(kP, kI, kD);
	}
	
	public void setSetpoint(int setpoint) {
		setAutomatic();
		elevationPID.setSetpoint(setpoint);
	}
	
	/**
	 * Sets the location of
	 * @param setpointPercent
	 */
	
//	public void setPercentSetpoint(double setpointPercent) {
//		double correctedSetpoint = POT_MULTIPLIER * setpointPercent + POT_OFFSET;		
//		elevationPID.setSetpoint(correctedSetpoint);
//	}
	
	public double getPotValue() {
		return pot.get();
	}
	
	public double getPercentHeight() {
		return (pot.get() - LOWEST_POS)/POS_RANGE;
	}
	
	/**
	 * Sets the claw to automatic control, PID will control the winch, moveManually will not function
	 */
	public void setAutomatic() {
		automatic = true;
		elevationPID.enable();
	}
	
	/**
	 * Sets the claw to manual control, PID will not control elevation, but the moveManually method will function. 
	 */
	public void setManual() {
		automatic = false;
		elevationPID.disable();
	}

	/**
	 * Returns if the winch is being controlled by PID.
	 */
	public boolean isAutomatic() {
		return automatic;
	}
	
	/**
	 * Returns if the winch can be controlled manually.
	 */
	public boolean isManual() {
		return !automatic;
	}
	
	public boolean isPidOnTarget() {
		return elevationPID.onTarget(); 
	}

}
