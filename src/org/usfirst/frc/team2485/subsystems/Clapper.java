package org.usfirst.frc.team2485.subsystems;

import org.usfirst.frc.team2485.util.CombinedVictorSP;
import org.usfirst.frc.team2485.util.InvertedPot;
import org.usfirst.frc.team2485.util.ThresholdHandler;

import edu.wpi.first.wpilibj.AnalogPotentiometer;
import edu.wpi.first.wpilibj.DoubleSolenoid;
import edu.wpi.first.wpilibj.PIDController;
import edu.wpi.first.wpilibj.VictorSP;

/**
 * @author Ben Clark
 * @author Aidan Fay
 */
public class Clapper {

	private CombinedVictorSP clapperLifter;
	private DoubleSolenoid clapperActuator;
	private PIDController clapperPID;
	private AnalogPotentiometer pot;
	private InvertedPot potInverted;
	
	private boolean open;
	private boolean automatic;

	public double
		kP	= 0.01,
		kI	= 0.00,
		kD	= 0.00;
			
	private static final double LOWEST_POS = 502; 
	private static final double POS_RANGE = 375;
	private static final double POT_TOLERANCE = 5;
	
	public static final double 
		ABOVE_RATCHET_SETPOINT		= LOWEST_POS + 140,
		ON_RATCHET_SETPOINT			= LOWEST_POS + 125, 
		LOADING_SETPOINT			= LOWEST_POS + 25,
		COOP_ZERO_TOTE_SETPOINT		= LOWEST_POS + 77, 
		COOP_ONE_TOTE_SETPOINT		= LOWEST_POS + 175, 
		COOP_TWO_TOTES_SETPOINT		= LOWEST_POS + 275,
		COOP_THREE_TOTES_SETPOINT	= LOWEST_POS + 370, 
		SCORING_PLATFORM_HEIGHT		= LOWEST_POS + 25;

	public Clapper(VictorSP clapperLifter1, VictorSP clapperLifter2,
			DoubleSolenoid clapperActuator, AnalogPotentiometer pot) {

		this.clapperLifter			= new CombinedVictorSP(clapperLifter1, clapperLifter2);
		this.clapperActuator		= clapperActuator;
		this.pot					= pot;
		
		this.potInverted			= new InvertedPot(pot);
		
		this.clapperPID = new PIDController(kP, kI, kD, potInverted, clapperLifter);
		this.clapperPID.setAbsoluteTolerance(POT_TOLERANCE);
		this.clapperPID.setOutputRange(-0.45, 0.6);
		
		this.automatic				= false;
		this.open					= false;
		
		clapperLifter.invertMotorDirection(true);
	}

	
	public Clapper(int clapperLifter1Port, int clapperLifter2Port, 
			int clapperActuatorPort1, int clapperActuatorPort2, int potPort) {

		this(new VictorSP(clapperLifter1Port), new VictorSP(clapperLifter2Port),
				new DoubleSolenoid(clapperActuatorPort1, clapperActuatorPort2),
				new AnalogPotentiometer(potPort));
	}
	
	public double getPotValue() {
		return potInverted.pidGet();
	}
	
	public void setPID(double kP, double kI, double kD) {
		this.kP = kP;
		this.kI = kI;
		this.kD = kD;
		
		clapperPID.setPID(kP, kI, kD);
	}
	
	public void setSetpoint(double setpoint) {
		setAutomatic();
		clapperPID.setSetpoint(setpoint);
	}
	
	public double getSetpoint() {
		return clapperPID.getSetpoint();
	}
	
	public boolean isPIDOnTarget() {
		return clapperPID.onTarget(); 
	}
	
	public void openClapper() {
		clapperActuator.set(DoubleSolenoid.Value.kForward);
		open = true;
	}

	public void closeClapper() {
		clapperActuator.set(DoubleSolenoid.Value.kReverse);
		open = false;
	}

	public boolean isOpen() {
		return open;
	}

	
	public double getPercentHeight() {
		return (pot.get() - LOWEST_POS)/POS_RANGE;
	}
	
	/**
	 * Sets the claw to automatic control, PID will control the winch, moveManually will not function
	 */
	public void setAutomatic() {
		automatic = true;
		clapperPID.enable();
	}
	
	/**
	 * Sets the claw to manual control, PID will not control elevation, but the moveManually method will function. 
	 */
	public void setManual() {
		automatic = false;
		clapperPID.disable();
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
	
	/*
	 * Assuming that a positive speed moves the clapper down
	 */
	public void liftManually(double speed) {
		setManual();
		double adjustedSpeed = ThresholdHandler.handleThreshold(speed, 0.1)/2;
		
		if (adjustedSpeed > 1){
			adjustedSpeed = 1;
		} else if (adjustedSpeed < -1){
			adjustedSpeed = -1;
		}
		clapperLifter.set(adjustedSpeed);
		System.out.println(speed + " | " + adjustedSpeed);

	}
}

	//  two belts for intake, pneumatic for finger, pneumatic for opens and closes whole intake, one pneumatic for open/closes 
	//	the belts, sensors for detecting tote
