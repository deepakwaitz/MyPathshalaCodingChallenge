package com.mypathshala.foodscanner.view

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mypathshala.foodscanner.R
import com.mypathshala.foodscanner.repository.FireStoreRepository.FIRE_STORE_DOCUMENT_KEY_INGREDIENTS
import com.mypathshala.foodscanner.repository.FireStoreRepository.FIRE_STORE_DOCUMENT_KEY_NAME
import com.mypathshala.foodscanner.utils.Utils
import com.mypathshala.foodscanner.utils.Utils.showSnackBar
import com.mypathshala.foodscanner.view.CameraActivity.Companion.BARCODE_BUNDLE_KEY
import com.mypathshala.foodscanner.view.CameraActivity.Companion.PRODUCT_NAME_BUNDLE_KEY
import com.mypathshala.foodscanner.viewModel.ProductViewModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_product.*

class ProductActivity : AppCompatActivity() {
    val TAG: String = ProductActivity::class.java.simpleName

    private var barCode: String? = null
    private var scannedIngredients: String? = ""
    private var updatedProductName: String? = null
    private var isFromTextScan: Boolean = false

    private val fireStoreDB = Firebase.firestore


    private lateinit var productViewModel: ProductViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product)
        barCode = intent.getStringExtra(BARCODE_BUNDLE_KEY)
        scannedIngredients = intent.getStringExtra(CameraActivity.INGREDIENTS_TEXT_BUNDLE_KEY)
        updatedProductName = intent.getStringExtra(CameraActivity.PRODUCT_NAME_BUNDLE_KEY)
        isFromTextScan = intent.getBooleanExtra(CameraActivity.IS_TEXT_SCAN, false)

        productViewModel = ViewModelProviders.of(this).get(ProductViewModel::class.java)

        product_name_edit_text?.setText(updatedProductName)

        ingredient_scan_button?.setOnClickListener {
            updatedProductName = product_name_edit_text?.text.toString()
            val cameraIntent = Intent(this, CameraActivity::class.java)
            cameraIntent.putExtra(MainActivity.IS_BARCODE_SCANNER, false)
            cameraIntent.putExtra(PRODUCT_NAME_BUNDLE_KEY, updatedProductName)
            cameraIntent.putExtra(BARCODE_BUNDLE_KEY, barCode)
            startActivity(cameraIntent)
            finish()
        }
        checkForProduct()
    }

    private fun addUpdateProduct(productName: String, ingredients: String) {
        if (Utils.isConnected(this)) {
            fireStoreDB.enableNetwork()
        } else {
            fireStoreDB.disableNetwork().addOnSuccessListener {
                finish()
            }.addOnFailureListener {
                showSnackBar(main_container, getString(R.string.error_msg))
            }
        }

        barCode?.let {
            productViewModel.onAddUpdateProduct(it, productName, ingredients).observe(this, Observer { isSuccessFull ->
                if (isSuccessFull)
                    finish()
                else
                    showSnackBar(product_container, getString(R.string.error_msg))
            })
        }
    }

    private fun checkForProduct() {
        barCode?.let { it ->
            productViewModel.getProduct(it).observe(this, Observer { productDocSnapShot ->
                when {
                    productDocSnapShot != null -> {
                        onProductFound(productDocSnapShot)
                        checkForAllergens()
                    }
                    isFromTextScan -> {
                        product_name_edit_text?.setText(updatedProductName)
                        product_ingredient_edit_text?.setText(scannedIngredients)
                        onAddProduct()
                    }
                    else -> {
                        showSnackBar(product_container, "Product Not Found")
                        showAddProductDialog()
                    }
                }
            })
        }
    }

    private fun onProductFound(result: DocumentSnapshot) {
        page_title?.text = getString(R.string.title_product_found)

        val productName = result.data?.get(FIRE_STORE_DOCUMENT_KEY_NAME) as String
        val ingredients = result.data?.get(FIRE_STORE_DOCUMENT_KEY_INGREDIENTS) as String

        if (!TextUtils.isEmpty(updatedProductName))
            product_name_edit_text?.setText(updatedProductName)
        else
            product_name_edit_text?.setText(productName)

        product_ingredient_edit_text?.setText(ingredients + scannedIngredients)

        add_update_product_button?.text = getString(R.string.placeholder_update)

        add_update_product_button?.setOnClickListener {
            val updatedProductName = product_name_edit_text?.text.toString()
            val updatedIngredients = product_ingredient_edit_text?.text.toString()
            addUpdateProduct(updatedProductName, updatedIngredients)
        }
    }

    private fun checkForAllergens() {
        var allergenicList: java.util.ArrayList<String>?

        productViewModel.getUserExceptionalList()?.observe(this, Observer {
            if (it != null && it.size > 0) {
                allergenicList = it
                fetchIngredients(allergenicList)
            } else
                showSnackBar(product_container, getString(R.string.error_msg))
        })
    }

    private fun fetchIngredients(allergenicList: ArrayList<String>?) {
        barCode?.let {
            productViewModel.getProductIngredients(it).observe(this, Observer { ingredients ->
                if (!TextUtils.isEmpty(ingredients))
                    isAllergic(allergenicList, ingredients)
                else
                    showSnackBar(product_container, getString(R.string.error_msg))
            })
        }
    }

    private fun isAllergic(allergenicList: ArrayList<String>?, ingredients: String?) {
        var isAllergic = false
        var allergicIngredients: String = ""
        if (!allergenicList.isNullOrEmpty() && !TextUtils.isEmpty(ingredients)) {
            for (allergen in allergenicList) {
                if (ingredients.toString().contains(allergen, true)) {
                    isAllergic = true
                    allergicIngredients += allergen
                }
            }
        }

        if (isAllergic) {
            allergen_note_card_view?.visibility = View.VISIBLE
            allergic_warning_layout?.setBackgroundColor(resources.getColor(R.color.light_red_color))
            allergic_note_tv?.text = getString(R.string.allergic_warning) + allergicIngredients
            ok_button?.setOnClickListener {
                finish()
            }
        } else {
            allergen_note_card_view?.visibility = View.VISIBLE
            allergic_warning_layout?.setBackgroundColor(resources.getColor(R.color.border_color))
            allergic_note_tv?.text = getString(R.string.no_allergic_warning)
            allergic_note_tv?.setTextColor(resources.getColor(R.color.colorPrimaryDark))
            ok_button?.setOnClickListener {
                finish()
            }
        }
    }

    private fun showAddProductDialog() {
        val exceptionAlertDialog = AlertDialog.Builder(this)

        exceptionAlertDialog.setTitle(getString(R.string.placeholder_unknown_product))
        exceptionAlertDialog.setMessage(getString(R.string.placeholder_unknown_product_message))

        exceptionAlertDialog.setPositiveButton(
            getString(R.string.placeholder_add)
        ) { _, _ ->
            onAddProduct()
        }

        exceptionAlertDialog.setNegativeButton(
            getString(R.string.placeholder_cancel)
        ) { _, _ ->
            finish()
        }
        exceptionAlertDialog.show()
    }

    private fun onAddProduct() {
        page_title?.text = getString(R.string.title_new_product)

        add_update_product_button?.setOnClickListener {
            val productName = product_name_edit_text?.text.toString()
            val ingredients = product_ingredient_edit_text?.text.toString()
            addUpdateProduct(productName, ingredients)
        }
    }
}
