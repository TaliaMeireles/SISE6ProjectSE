package pt.ulisboa.tecnico.learnjava.sibs.domain;

import pt.ulisboa.tecnico.learnjava.bank.exceptions.AccountException;
import pt.ulisboa.tecnico.learnjava.bank.services.Services;
import pt.ulisboa.tecnico.learnjava.sibs.exceptions.OperationException;
import pt.ulisboa.tecnico.learnjava.sibs.exceptions.SibsException;
import pt.ulisboa.tecnico.learnjava.sibs.state.Canceled;
import pt.ulisboa.tecnico.learnjava.sibs.state.Completed;
import pt.ulisboa.tecnico.learnjava.sibs.state.Retry;
import pt.ulisboa.tecnico.learnjava.sibs.state.Error;

public class Sibs {
	private final Operation[] operations;
	private Services services;

	public Sibs(int maxNumberOfOperations, Services services) {
		this.operations = new Operation[maxNumberOfOperations];
		this.services = services;
	}

	public void transfer(String sourceIban, String targetIban, int amount)
			throws SibsException, AccountException, OperationException {
		if (!this.services.accountExists(sourceIban) || !this.services.accountExists(targetIban) ||
				this.services.accountInactive(sourceIban) || this.services.accountInactive(targetIban)) {
			throw new SibsException();
		}
		
		TransferOperation operation = new TransferOperation(sourceIban, targetIban, amount);
		
		addOperation(operation);
	}
	
	public void processOperations() throws OperationException, AccountException {
		for(Operation operation : this.operations) {
			if(operation != null && operation.getType().equals(Operation.OPERATION_TRANSFER)) {
				TransferOperation transfer = (TransferOperation) operation;
				if(nonFinalState(transfer)){
					tryProcess(transfer);
				}
			}
		}
	}
	
	public void tryProcess(TransferOperation transfer) throws AccountException, OperationException {
		try {
			transfer.process(services);
		} catch (Exception e) {
			if (!(transfer.getState() instanceof Retry)) {
				(new Retry()).process(transfer, services);
			}
		}
	}
	
	public void cancelOperation(int id) throws OperationException, AccountException, SibsException {
		if (getOperation(id) == null) {
			throw new SibsException("Error in \"cancelOperation\" method! The id " + id + " returns null.");
		}
		
		if(getOperation(id).getType().equals(Operation.OPERATION_TRANSFER)) {
			TransferOperation transfer = (TransferOperation) getOperation(id);
			if(nonFinalState(transfer)){
				transfer.cancel(this.services);
			}
		}
	}
	
	public boolean nonFinalState(TransferOperation transfer) {
		return !(transfer.getState() instanceof Completed) 
				&& !(transfer.getState() instanceof Canceled)
				&& !(transfer.getState() instanceof Error);
	}

	public int addOperation(Operation operation) throws OperationException, SibsException {
		int position = -1;
		for (int i = 0; i < this.operations.length; i++) {
			if (this.operations[i] == null) {
				position = i;
				break;
			}
		}

		if (position == -1) {
			throw new SibsException();
		}

		this.operations[position] = operation;
		return position;
	}

	public void removeOperation(int position) throws SibsException {
		if (position < 0 || position > this.operations.length) {
			throw new SibsException();
		}
		this.operations[position] = null;
	}

	public Operation getOperation(int position) throws SibsException {
		if (position < 0 || position > this.operations.length) {
			throw new SibsException("Error in \"cancelOperation\" method! The id " + 
					position + " it is out of limits.");
		}
		return this.operations[position];
	}

	public int getNumberOfOperations() {
		int result = 0;
		for (int i = 0; i < this.operations.length; i++) {
			if (this.operations[i] != null) {
				result++;
			}
		}
		return result;
	}

	public int getTotalValueOfOperations() {
		int result = 0;
		for (int i = 0; i < this.operations.length; i++) {
			if (this.operations[i] != null) {
				result = result + this.operations[i].getValue();
			}
		}
		return result;
	}

	public int getTotalValueOfOperationsForType(String type) {
		int result = 0;
		for (int i = 0; i < this.operations.length; i++) {
			if (this.operations[i] != null && this.operations[i].getType().equals(type)) {
				result = result + this.operations[i].getValue();
			}
		}
		return result;
	}
}
