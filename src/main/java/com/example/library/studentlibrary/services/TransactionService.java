package com.example.library.studentlibrary.services;

import com.example.library.studentlibrary.models.*;
import com.example.library.studentlibrary.repositories.BookRepository;
import com.example.library.studentlibrary.repositories.CardRepository;
import com.example.library.studentlibrary.repositories.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class TransactionService {

    @Autowired
    BookRepository bookRepository5;

    @Autowired
    CardRepository cardRepository5;

    @Autowired
    TransactionRepository transactionRepository5;

    @Value("${books.max_allowed}")
    int max_allowed_books;

    @Value("${books.max_allowed_days}")
    int getMax_allowed_days;

    @Value("${books.fine.per_day}")
    int fine_per_day;

    public String issueBook(int cardId, int bookId) throws Exception {
        //check whether bookId and cardId already exist
        //conditions required for successful transaction of issue book:
        //1. book is present and available
        // If it fails: throw new Exception("Book is either unavailable or not present");
        //2. card is present and activated
        // If it fails: throw new Exception("Card is invalid");
        //3. number of books issued against the card is strictly less than max_allowed_books
        // If it fails: throw new Exception("Book limit has reached for this card");
        //If the transaction is successful, save the transaction to the list of transactions and return the id

        //Note that the error message should match exactly in all cases

        if(bookRepository5.existsById(bookId)){

            if(cardRepository5.existsById(cardId)){
                List<Book> books= new ArrayList<>();
                books = cardRepository5.findById(cardId).get().getBooks();

                if(books.size()< max_allowed_books){

                    Transaction transaction = new Transaction();
                    transaction.setBook(bookRepository5.findById(bookId).get());
                    transaction.setCard(cardRepository5.findById(cardId).get());
                    transaction.setIssueOperation(true);
                    transaction.setTransactionStatus(TransactionStatus.SUCCESSFUL);

                    bookRepository5.findById(bookId).get().setCard(cardRepository5.findById(cardId).get());
                    List<Transaction> transactionList = bookRepository5.findById(bookId).get().getTransactions();
                    transactionList.add(transaction);
                    bookRepository5.findById(bookId).get().setTransactions(transactionList);
                    bookRepository5.findById(bookId).get().setAvailable(false);

                    books.add(bookRepository5.findById(bookId).get());
                    cardRepository5.findById(cardId).get().setBooks(books);
                    cardRepository5.save(cardRepository5.findById(cardId).get());

                    return transaction.getTransactionId();

                }else{
                    throw new Exception("Book limit has reached for this card");
                }
            }else {
                throw new Exception("Card is invalid");
            }

        }else{
            throw new Exception("Book is either unavailable or not present");
        }

      //return transactionId instead
    }

    public Transaction returnBook(int cardId, int bookId) throws Exception{

        List<Transaction> transactions = transactionRepository5.find(cardId, bookId,TransactionStatus.SUCCESSFUL, true);
        Transaction transaction = transactions.get(transactions.size() - 1);

        //for the given transaction calculate the fine amount considering the book has been returned exactly when this function is called
        //make the book available for other users
        //make a new transaction for return book which contains the fine amount as well
        int dayCount = 0;
        Date issueDate = transaction.getTransactionDate();
        Date currentDate = new Date();
        long diffDate = currentDate.getTime() - issueDate.getTime();
        dayCount = (int)diffDate/86400000;
        int diffDayCount = dayCount - getMax_allowed_days;
        if(diffDayCount < 0){
            diffDayCount = 0;
        }

        bookRepository5.findById(bookId).get().setAvailable(true);
        bookRepository5.save(bookRepository5.findById(bookId).get());

        Transaction returnBookTransaction  = new Transaction() ;
        returnBookTransaction.setBook(bookRepository5.findById(bookId).get());
        returnBookTransaction.setTransactionStatus(TransactionStatus.SUCCESSFUL);
        returnBookTransaction.setIssueOperation(false);
        returnBookTransaction.setFineAmount(diffDayCount * fine_per_day);
        transactionRepository5.save(returnBookTransaction);

        return returnBookTransaction; //return the transaction after updating all details
    }
}