package com.hihonor.iap.demo1;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.hihonor.iap.framework.data.ApiException;
import com.hihonor.iap.framework.utils.JsonUtil;
import com.hihonor.iap.sdk.Iap;
import com.hihonor.iap.sdk.IapClient;
import com.hihonor.iap.sdk.bean.CancelSubscriptionResultReq;
import com.hihonor.iap.sdk.bean.ConsumeReq;
import com.hihonor.iap.sdk.bean.ConsumeResult;
import com.hihonor.iap.sdk.bean.OwnedPurchasesReq;
import com.hihonor.iap.sdk.bean.OwnedPurchasesResult;
import com.hihonor.iap.sdk.bean.ProductInfoReq;
import com.hihonor.iap.sdk.bean.ProductInfoResult;
import com.hihonor.iap.sdk.bean.ProductOrderIntentReq;
import com.hihonor.iap.sdk.bean.ProductOrderIntentResult;
import com.hihonor.iap.sdk.bean.ProductOrderIntentWithPriceReq;
import com.hihonor.iap.sdk.bean.ProductType;
import com.hihonor.iap.sdk.bean.PurchaseProductInfo;
import com.hihonor.iap.sdk.bean.PurchaseResultInfo;
import com.hihonor.iap.sdk.tasks.OnFailureListener;
import com.hihonor.iap.sdk.tasks.OnSuccessListener;
import com.hihonor.iap.sdk.tasks.Task;
import com.hihonor.iap.sdk.utils.IapUtil;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final int REQUEST_CODE_PAY = 1001;

    private String continueToken = "";
    private String continueTokenRecord = "";
    private String agreementNo = ""; // 合约号
    private String iapOrderNo = ""; // 支付成功时返回的荣耀订单号
    private String bizOrderNo = "123"; // 业务订单号
    private String productName = "123"; // 商品名称
    private long price = 1; // 商品价格

    private IapClient iapClient;

    private TextView tvLogInfo;
    private TextView tvConsumeLog;
    private Switch mSwNeedSandboxTest;
    private RadioGroup mRadioGroup;
    private EditText etProductId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvLogInfo = findViewById(R.id.tv_logInfo);
        tvConsumeLog = findViewById(R.id.tv_consume_log);
        mSwNeedSandboxTest = findViewById(R.id.swNeedSandboxTest);
        mRadioGroup = findViewById(R.id.radioGroup);
        etProductId = findViewById(R.id.et_productId);

        String appId = "2*******5";
        String cpId = "41******************61";
        iapClient = Iap.getIapClient(this, appId, cpId);

        TextWatcher textWatcher = new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                tvConsumeLog.setText("");
            }

        };
        tvLogInfo.addTextChangedListener(textWatcher);

        // 查询商品信息 在荣耀开发者平台的商品管理系统（PMS）中配置商品时，建议调用iapClient.getProductInfo接口获取商品信息
        findViewById(R.id.bt_query_product).setOnClickListener(v -> {
            if (TextUtils.isEmpty(etProductId.getText().toString())){
                Toast.makeText(MainActivity.this, "请输入商品ID", Toast.LENGTH_SHORT).show();
                return;
            }
            int productType = ProductType.CONSUME;
            if (mRadioGroup.getCheckedRadioButtonId() == R.id.radio_consumed_product) {
                productType = ProductType.CONSUME;// 消耗型
            } else if (mRadioGroup.getCheckedRadioButtonId() == R.id.radio_unconsumed_product) {
                productType = ProductType.PERMANENT;// 非消耗型
            } else {
                productType = ProductType.SUBSCRIPTION;// 订阅型
            }
            ProductInfoReq productInfoReq = new ProductInfoReq();
            // ProductType.CONSUME表示消耗型商品,ProductType.PERMANENT表示非消耗型商品,ProductType.SUBSCRIPTION表示订阅型商品。传入的商品ID和商品类型要对应
            productInfoReq.setProductType(productType);
            List<String> list = new ArrayList<String>(); // 一次性传入的商品ID不能超过10个。
            list.add(etProductId.getText().toString());
            productInfoReq.setProductIds(list);
            Task<ProductInfoResult> productInfo = iapClient.getProductInfo(productInfoReq);
            productInfo.addOnSuccessListener(productInfoResult -> {
                tvLogInfo.setText("已刷新" + productInfoResult.getProductInfos().size() + " 条数据  " + productInfoResult.getProductInfos().toString());
                Log.e(TAG, "product data is：" + productInfoResult.getProductInfos().toString());
                // 海外接入Toast需删除，或者配置本地化语言
                Toast.makeText(MainActivity.this, "已刷新" + productInfoResult.getProductInfos().size() + " 条数据", Toast.LENGTH_SHORT).show();
            }).addOnFailureListener(e -> {
                tvLogInfo.setText(e.errorCode + "  " + e.message);
                Log.e(TAG, String.format("getProductInfo %d %s", e.errorCode, e.message));
            });
        });

        // 查询已购买未消耗的列表
        findViewById(R.id.bt_obtainPurchased).setOnClickListener(v -> {
            int productType = ProductType.CONSUME;
            if (mRadioGroup.getCheckedRadioButtonId() == R.id.radio_consumed_product) {
                productType = ProductType.CONSUME;// 消耗型
            } else if (mRadioGroup.getCheckedRadioButtonId() == R.id.radio_unconsumed_product) {
                productType = ProductType.PERMANENT;// 非消耗型
            } else {
                productType = ProductType.SUBSCRIPTION;// 订阅型
            }
            OwnedPurchasesReq ownedPurchasesReq = new OwnedPurchasesReq();
            ownedPurchasesReq.setProductType(productType);
            // 传入上一次查询得到的continueToken，获取新的数据，第一次传空
            ownedPurchasesReq.setContinuationToken(continueToken);
            iapClient.obtainOwnedPurchases(ownedPurchasesReq).addOnSuccessListener(new OnSuccessListener<OwnedPurchasesResult>() {
                @Override
                public void onSuccess(OwnedPurchasesResult ownedPurchasesResult) {
                    tvLogInfo.setText(ownedPurchasesResult.toString());
                    Log.d(TAG, ownedPurchasesResult.toString());
                    // ContinueToken用于获取下一个列表的数据，第一次为空，如果有更多数据ContinueToken有值，为空则没有更多数据
                    continueToken = ownedPurchasesResult.getContinueToken();

                    if (mRadioGroup.getCheckedRadioButtonId() == R.id.radio_consumed_product) {
                        // 对购买数据进行验签 purchaseList 和 sigList 一一对应
                        verifyPublicKey(ownedPurchasesResult);
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(ApiException e) {
                    tvLogInfo.setText(e.errorCode + "  " + e.message);
                    // e.errorCode 对应 OrderStatusCode的值
                    Log.e(TAG, String.format("createProductOrderIntent %d %s", e.errorCode, e.message));
                }
            });
        });

        // 创建订单  购买PMS商品时，需要调用iapClient.createProductOrderIntent接口
        findViewById(R.id.bt_createProductOrder).setOnClickListener(v -> {
            if (TextUtils.isEmpty(etProductId.getText().toString())){
                Toast.makeText(MainActivity.this, "请输入商品ID", Toast.LENGTH_SHORT).show();
                return;
            }
            int needSandboxTest = mSwNeedSandboxTest.isChecked() ? 1 : 0;
            int productType = ProductType.CONSUME;
            if (mRadioGroup.getCheckedRadioButtonId() == R.id.radio_consumed_product) {
                productType = ProductType.CONSUME;// 消耗型
            } else if (mRadioGroup.getCheckedRadioButtonId() == R.id.radio_unconsumed_product) {
                productType = ProductType.PERMANENT;// 非消耗型
            } else {
                productType = ProductType.SUBSCRIPTION;// 订阅型
            }

            ProductOrderIntentReq productOrderIntentReq = new ProductOrderIntentReq();
            productOrderIntentReq.setProductType(productType);
            productOrderIntentReq.setProductId(etProductId.getText().toString());
            productOrderIntentReq.setNeedSandboxTest(needSandboxTest);//传1为沙盒测试
            // 防止掉单
            // 创建订单前，需要调用obtainOwnedPurchases 查询已购买，未消耗的商品，进行消耗
            Task<ProductOrderIntentResult> productOrderIntent = iapClient.createProductOrderIntent(productOrderIntentReq);
            productOrderIntent.addOnSuccessListener(createProductOrderResp -> {
                Intent intent = createProductOrderResp.getIntent();
                if (intent != null) {
                    // createProductOrderIntent接口调用成功，需要调用startActivityForResult方法去拉起收银台，在Activity的onActivityResult生命周期方法中处理支付返回的结果
                    startActivityForResult(intent, REQUEST_CODE_PAY);
                }
            }).addOnFailureListener(e -> {
                // e.errorCode 对应 OrderStatusCode的值
                tvLogInfo.setText("createProductOrderIntent %d %s " + e.errorCode + " " + e.message);
                Log.e(TAG, String.format("createProductOrderIntent %d %s", e.errorCode, e.message));
            });
        });

        // 创建订单  购买非PMS商品时，需要调用iapClient.createProductOrderIntentWithPrice接口,该方式暂不支持购买订阅型商品
        findViewById(R.id.bt_createProductOrderWithPrice).setOnClickListener(v -> {
            if (TextUtils.isEmpty(etProductId.getText().toString())){
                Toast.makeText(MainActivity.this, "请输入商品ID", Toast.LENGTH_SHORT).show();
                return;
            }
            int needSandboxTest = mSwNeedSandboxTest.isChecked() ? 1 : 0;
            int productType = ProductType.CONSUME;
            if (mRadioGroup.getCheckedRadioButtonId() == R.id.radio_consumed_product) {
                productType = ProductType.CONSUME;// 消耗型
            } else if (mRadioGroup.getCheckedRadioButtonId() == R.id.radio_unconsumed_product) {
                productType = ProductType.PERMANENT;// 非消耗型
            } else {
                Toast.makeText(MainActivity.this, "自定义商品购买暂不支持订阅型商品，请选择其他类型商品", Toast.LENGTH_SHORT).show();
                return;
            }
            ProductOrderIntentWithPriceReq productOrderIntentWithPriceReq = new ProductOrderIntentWithPriceReq();
            productOrderIntentWithPriceReq.setProductType(productType);//商品类型，0是消耗型商品，1为非消耗型商品
            productOrderIntentWithPriceReq.setProductId(etProductId.getText().toString());//商品ID
            productOrderIntentWithPriceReq.setBizOrderNo(bizOrderNo);//业务订单号,可以理解为游戏或app自定义订单号
            productOrderIntentWithPriceReq.setCurrency("CNY");//币种，中国：CNY
            productOrderIntentWithPriceReq.setPrice(price);// 商品价格，需传入*100以后的值，商品价格为1元时，此处传参100
            productOrderIntentWithPriceReq.setProductName(productName);//商品名称
//            productOrderIntentWithPriceReq.setDeveloperPayload("12344556778989");//商户侧保留信息，支付结果会按传入内容返回
            productOrderIntentWithPriceReq.setNeedSandboxTest(needSandboxTest);//传1为沙盒测试，0为正式支付
//            productOrderIntentWithPriceReq.setSecondChargeTime(secondChargeTime);//第二次扣费时间（订阅型商品时传入）,格式yyyy-MM-dd
//            productOrderIntentWithPriceReq.setSubPeriod(1);// 订购周期，productType为2时必传。
//            productOrderIntentWithPriceReq.setPeriodUnit("W");// 订购周期单位（W：周，M：月，Y：年。订阅商品有效）
//            productOrderIntentWithPriceReq.setPromotionPrice(1);// 优惠价格，需传入*100以后的值，比如价格为1元，需传入100
            //以上具体参数介绍参考《荣耀应用内支付SDK接口文档》
            //防止掉单
            //创建订单前，需要调用obtainOwnedPurchases 查询已购买，未消耗的商品，进行消耗
            Task<ProductOrderIntentResult> productOrderIntent = iapClient.
                    createProductOrderIntentWithPrice(productOrderIntentWithPriceReq);
            productOrderIntent.addOnSuccessListener(createProductOrderResp -> {
                Intent intent = createProductOrderResp.getIntent();
                if (intent != null) {
                    // createProductOrderIntentWithPrice接口调用成功，需要调用startActivityForResult方法去拉起收银台，在Activity的onActivityResult生命周期方法中处理支付返回的结果
                    startActivityForResult(intent, REQUEST_CODE_PAY);
                }
            }).addOnFailureListener(e -> {
                //e.errorCode 对应 OrderStatusCode的值
                tvLogInfo.setText("createProductOrderIntentWithPrice %d %s " + e.errorCode + " " + e.message);
                Log.e(TAG, String.format("createProductOrderIntentWithPrice %d %s", e.errorCode,
                        e.message));
            });
        });

        // 查询已购买的列表
        findViewById(R.id.bt_obtainPurchaseRecord).setOnClickListener(v -> {
            int productType = ProductType.CONSUME;
            if (mRadioGroup.getCheckedRadioButtonId() == R.id.radio_consumed_product) {
                productType = ProductType.CONSUME;// 消耗型
            } else if (mRadioGroup.getCheckedRadioButtonId() == R.id.radio_unconsumed_product) {
                productType = ProductType.PERMANENT;// 非消耗型
            } else {
                productType = ProductType.SUBSCRIPTION;// 订阅型
            }
            OwnedPurchasesReq ownedPurchasesReq = new OwnedPurchasesReq();
            ownedPurchasesReq.setProductType(productType);//商品类型，0是消耗型商品，1为非消耗型商品，2为订阅型商品
            // 传入上一次查询得到的continueToken，获取新的数据，第一次传空
            ownedPurchasesReq.setContinuationToken(continueTokenRecord);
            iapClient.obtainOwnedPurchaseRecord(ownedPurchasesReq).addOnSuccessListener(new OnSuccessListener<OwnedPurchasesResult>() {
                @Override
                public void onSuccess(OwnedPurchasesResult ownedPurchasesResult) {
                    tvLogInfo.setText(ownedPurchasesResult.toString());
                    // 获取到结果后需要进行签名校验
                    // ContinueToken用于获取下一个列表的数据，第一次为空，如果有更多数据ContinueToken有值，为空则没有更多数据
                    continueTokenRecord = ownedPurchasesResult.getContinueToken();
                    Log.d(TAG, "obtainOwnedPurchaseRecord：" + ownedPurchasesResult.toString());
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(ApiException e) {
                    // e.errorCode 对应 OrderStatusCode的值
                    tvLogInfo.setText("obtainOwnedPurchaseRecord %d %s " + e.errorCode + e.message);
                    Log.e(TAG, String.format("obtainOwnedPurchaseRecord %d %s", e.errorCode, e.message));
                }
            });
        });

        // 取消订阅
        findViewById(R.id.bt_cancelSubscription).setOnClickListener(v -> {
            if (TextUtils.isEmpty(agreementNo) && TextUtils.isEmpty(iapOrderNo)){
                Toast.makeText(MainActivity.this, "您暂时没有订阅型商品的订单号和合约号，请配置后重新尝试", Toast.LENGTH_SHORT).show();
                return;
            }
            CancelSubscriptionResultReq cancelSubscriptionResultReq = new CancelSubscriptionResultReq();
            cancelSubscriptionResultReq.setCancelType(2);//固定值，值为2。代表通过SDK提供的cancelSubscriptionProduct调用。
            cancelSubscriptionResultReq.setAgreementNo(agreementNo);//合约号，从获取的已购订阅商品信息中获取
            cancelSubscriptionResultReq.setIapOrderNo(iapOrderNo);//支付成功时生成的订单号
            Task<Object> cancelResultTask = iapClient.cancelSubscriptionProduct(cancelSubscriptionResultReq);
            cancelResultTask.addOnSuccessListener(o -> {
                tvLogInfo.setText("cancelSubscription success");
                Log.d(TAG, "cancelSubscription success");
            });
            cancelResultTask.addOnFailureListener(e -> {
                tvLogInfo.setText("cancelSubscription fail " + e.errorCode + e.message);
                Log.e(TAG, String.format("cancelSubscription fail  %d %s", e.errorCode, e.message));
            });
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PAY) {
            // 客户端并不能100%确保支付结果回调
            // 当没收到回调时，可调用getOwnedPurchased、getOwnedPurchaseRecord查询购买记录
            if (resultCode == Activity.RESULT_OK) {
                PurchaseResultInfo purchaseResultInfo = IapUtil.parsePurchaseResultInfoFromIntent(data);
                if (purchaseResultInfo == null) {
                    // 取消支付
                } else {
                    String purchaseProductInfoStr = purchaseResultInfo.getPurchaseProductInfo();
                    Log.d(TAG, "---onActivityResult---purchaseProductInfoStr---" + purchaseProductInfoStr);
                    try {
                        PurchaseProductInfo purchaseProductInfo = JsonUtil.parse(purchaseProductInfoStr, PurchaseProductInfo.class);
                        switch (purchaseProductInfo.getPurchaseState()) {
                            case PurchaseProductInfo.PurchaseState.PAID:
                                // 支付成功
                                tvLogInfo.setText("---onActivityResult---支付成功---" + purchaseProductInfo);
                                int productType = purchaseProductInfo.getProductType();
                                if (0 == productType) {
                                    PublicKey publicKey = RSAUtil.getPublicKey(BuildConfig.PUBLIC_KEY);
                                    Log.d(TAG, "onActivityResult publicKey :" + publicKey);
                                    boolean verify = RSAUtil.verify(purchaseProductInfoStr, publicKey, purchaseResultInfo.getPurchaseProductInfoSig());
                                    if (verify) {
                                        // 对消耗型商品进行发货，发货成功之后，进行消耗。也就是通知荣耀服务器商品已发货。消耗之后才能重复购买。
                                        consumeProduct(purchaseProductInfo.getPurchaseToken());
                                    }
                                } else if (2 == productType) {
                                    agreementNo = purchaseProductInfo.getSubscription().getAgreementNo();
                                    iapOrderNo = purchaseProductInfo.getSubscription().getSubIapOrderNo();
                                }
                                break;
                            case PurchaseProductInfo.PurchaseState.UNPAID:
                            case PurchaseProductInfo.PurchaseState.PAID_FAILED:
                            default:
                                // 支付失败
                                tvLogInfo.setText("---onActivityResult---支付失败---");
                                Log.d(TAG, "---onActivityResult---支付失败---");
                        }
                    } catch (Throwable t) {
                        // 支付失败
                        tvLogInfo.setText("---onActivityResult---支付失败---" + t.getMessage());
                        Log.e(TAG, "---onActivityResult---支付失败---" + t.getMessage());
                    }
                }
            } else {
                // 取消支付
                tvLogInfo.setText("---onActivityResult---取消支付---");
                Log.d(TAG, "---onActivityResult---取消支付---");
            }
        }
    }

    /**
     * 商品消耗
     *
     * @param purchaseToken
     */
    private void consumeProduct(String purchaseToken) {
        ConsumeReq comsumeReq = new ConsumeReq();
        //根据PurchaseToken 进行消耗
        comsumeReq.setPurchaseToken(purchaseToken);
        Task<ConsumeResult> comsumeRespTask = iapClient.consumeProduct(comsumeReq);
        comsumeRespTask.addOnSuccessListener(comsumeResp -> {
            tvConsumeLog.setText("消耗成功：" + comsumeResp.getConsumeData());
            // 消耗成功
        }).addOnFailureListener(e -> {
            tvConsumeLog.setText("消耗失败：" + e.getErrorCode() + ": " + e.getMessage());
            // 消耗失败
            Toast.makeText(MainActivity.this, "消耗失败：" + e.getErrorCode() + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void verifyPublicKey(OwnedPurchasesResult ownedPurchasesResult) {
        List<String> sigList = ownedPurchasesResult.getSigList();
        List<String> purchaseList = ownedPurchasesResult.getPurchaseList();
        try {
            PublicKey publicKey = RSAUtil.getPublicKey(BuildConfig.PUBLIC_KEY);
            Log.d(TAG, " publicKey :" + publicKey);
            for (int i = 0; i < purchaseList.size(); i++) {
                String purchaseProductInfoStr = purchaseList.get(i);
                boolean verify = RSAUtil.verify(purchaseProductInfoStr, publicKey, sigList.get(i));
                // verify 为true数据可信，false数据被篡改了，数据不可信
                Log.d(TAG, " PurchaseProductInfoStr verify " + verify + "  , " + purchaseProductInfoStr);
                Log.d(TAG, " PurchaseProductInfoStr sign ，  " + sigList.get(i));
                if (verify) {
                    // 对消耗型商品进行发货，发货成功之后，进行消耗。也就是通知荣耀服务器商品已发货。消耗之后才能重复购买。
                    PurchaseProductInfo purchaseProductInfo = JsonUtil.parse(purchaseProductInfoStr, PurchaseProductInfo.class);
                    int productType = purchaseProductInfo.getProductType();
                    if (0 == productType) {
                        consumeProduct(purchaseProductInfo.getPurchaseToken());
                    }

                }
            }
        } catch (Exception e) {
            tvConsumeLog.setText("消耗失败：" + e.getMessage());
        }
    }
}
